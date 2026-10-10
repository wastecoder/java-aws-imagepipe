# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project context

`imagepipe` is an **event-driven image pipeline on AWS**, built as a learning/portfolio project. A client
requests an upload, receives an **S3 presigned URL**, and uploads the bytes directly to S3. The object-created
event flows through **SQS** to a **Lambda worker**, which generates image variants, writes them to a second
bucket, and updates PostgreSQL. The variants are served through **CloudFront**. Backend only — clients are
simulated (REST, Postman, tests).

The directory is named `aws-images`; the project, Gradle artifact, and root package are named **`imagepipe`**
(`com.wastecoder.imagepipe`).

**The domain is a pretext.** The purpose of this project is to learn the core of AWS in depth. It was chosen
because every core AWS service appears here out of necessity rather than decoration. Optimize decisions for
*learning value* and *cost*, never for domain sophistication.

The brief (what and why) lives in `docs/PROJECT.md`. The MVP roadmap lives in `docs/PROGRESS.md`; the post-MVP
vision in `docs/FUTURE-PROGRESS.md`.

## Language convention

- **Code** — classes, methods, variables, package names, comments, logs, commit messages, branch names, PR
  titles and descriptions — is written in **English**.
- **Documentation** — everything under `docs/` (including `docs/adr/`) and the `README.md` — is written in
  **Portuguese**.
- **This file** (`CLAUDE.md`) is written in **English**.

## Repository layout

Gradle multi-project (Kotlin DSL) monorepo. Deliberately a **modular monolith plus one async consumer** — not
microservices. Adding services would multiply infrastructure cost and teach nothing new about AWS.

These subprojects are the **build axis**, not the hexagon. Hexagonal architecture lives on the **package axis**,
*inside* each subproject — see the next section. The two are orthogonal.

| Subproject | Responsibility | Root package |
|---|---|---|
| `api` | Spring Boot REST API: issues presigned URLs, serves metadata, owns the DB schema | `com.wastecoder.imagepipe.api` |
| `worker` | AWS Lambda handler: consumes SQS, generates variants, updates the DB | `com.wastecoder.imagepipe.worker` |
| `shared` | Domain models and the S3 event contract. Plain Java, no framework | `com.wastecoder.imagepipe.shared` |
| `infra` | Terraform (modules: `network`, `data`, `compute`, `cdn`, `observability`) | — |

Shared build config lives in the root `build.gradle.kts`. Subprojects are declared in `settings.gradle.kts`.

**The `worker` has no Spring.** It is a plain `RequestHandler<SQSEvent, SQSBatchResponse>` using AWS SDK v2 and
JDBC. Spring in an async consumer only inflates cold start and package size. Do not add it. No framework, however,
is not an excuse for no architecture: the `worker` gets the same hexagon as everything else.

**The `shared` module is a domain kernel with one hard rule: its `build.gradle.kts` declares zero dependencies.**
No Spring, no AWS SDK, no JDBC. It holds only `ImageStatus`, `VariantSize`, and `StorageKeys` (the deterministic
key derivation that `api` needs to build CloudFront URLs and `worker` needs to write variants). If something does
not fit that rule, it belongs in `api` or in `worker`, not here. Aggregates (`Image`, `ImageVariant`) live in
`api`'s domain, not in `shared`.

## Architecture (hexagonal, horizontal layout)

Every module uses the **horizontal** layout: the top-level package is the **layer**, not the feature. Dependencies
point **inward** (`adapter -> application -> domain`). The `domain` package has **no** Spring, JPA, or AWS SDK
imports.

Horizontal, not vertical (as in `picpay-simplificado`), because `imagepipe` has a **single aggregate**, `Image`.
Both endpoints speak of it. With one feature, a vertical slice would reproduce the horizontal layout one level
down and buy nothing. Revisit only if genuinely independent features appear.

Packages below are relative to each subproject's root package `com.wastecoder.imagepipe.<module>`.

| Layer | Package | Naming and examples |
|---|---|---|
| Domain | `domain.model`, `domain.exception` | `Image`, `ImageVariant`; `*Exception` |
| Driving ports | `application.port.in` | `*UseCase` (e.g. `RequestUploadUseCase`) |
| Driven ports | `application.port.out` | `ImageRepository`, `PresignedUrlGenerator`, `ObjectStore` |
| Use cases | `application.usecase` | `*UseCaseImpl` |
| Boundary DTOs | `application.viewmodel` | `*Command`, `*View` |
| Web adapter (in) | `adapter.web` (`dto.request`/`dto.response`, `handler`) | `*Controller`; `GlobalExceptionHandler` and `ProblemType` in `handler` |
| Lambda adapter (in) | `adapter.lambda` | `SqsEventHandler`; the S3 event record is a wire DTO and lives here, not in `shared` |
| Persistence adapter (out) | `adapter.persistence` (`entity`/`mapper`/`database`) | `*RepositoryImpl` at root; `*Entity`, `*EntityMapper`, `*JpaDatabase` |
| Storage adapter (out) | `adapter.storage` | `S3PresignedUrlGenerator`, `S3ObjectStore` |
| CDN adapter (out) | `adapter.cdn` | `CloudFrontUrlResolver` |
| Config | `adapter.config` | `*Config` |

The `worker`'s hexagon carries the project's central lesson in its structure: **`TransientProcessingException` and
`PermanentProcessingException` are domain concepts.** Only `adapter.lambda` knows that one means "return the
message to the queue" and the other means "acknowledge it". Never leak that decision into the use case, and never
let the use case know what SQS is.

## Commands (Windows: use `gradlew.bat`)

- Build everything: `./gradlew build`
- Unit tests only: `./gradlew test`
- Integration tests (Testcontainers): `./gradlew integrationTest`
- Full verification and coverage gates: `./gradlew check`
- Mutation testing: `./gradlew pitest`
- Run the API: `./gradlew :api:bootRun`
- Local environment (PostgreSQL + LocalStack): `docker compose up -d` / stop: `docker compose down`
- Infrastructure: `terraform -chdir=infra plan` / `apply` / `destroy`

Prefer the commands above. **Do not run `git log --oneline` to infer commit conventions** — they are documented
in this file.

## Cost discipline (read before touching `infra/`)

The AWS account is on the **Free plan** with a finite credit balance. The environment is **disposable**: it goes
up with `terraform apply` to study one piece, and comes down with `terraform destroy` right after. Day-to-day
development happens locally against Docker Compose and LocalStack.

Hard rules:

- **Never create an `aws_nat_gateway`.** The network is designed so it is not needed. The Lambda reaches S3
  through a free **VPC Gateway Endpoint**; it needs no endpoint for CloudWatch Logs (the Lambda service writes
  on its behalf) or for SQS (the poller runs outside the VPC).
- **Never create a database password.** RDS uses **IAM authentication**; the token is signed locally with SigV4.
- **Never introduce an Interface Endpoint** without saying so explicitly — they cost roughly US$ 7/month per AZ.
- Before adding any resource with an hourly charge, state the monthly cost and ask.
- The two known expensive resources in this project are the **ALB** (about US$ 16-18/month) and the **RDS
  instance** (about US$ 14/month). Both are intentional. Nothing else should surprise the bill.

## AWS architecture

- The `api` runs on **ECS Fargate in a public subnet**, with a security group that only accepts traffic from the
  ALB's security group. Security groups reference **security groups, never CIDR blocks**.
- The `worker` Lambda runs in a **private subnet** because it needs RDS. Inside the VPC it loses internet access;
  it reaches S3 via the Gateway Endpoint.
- **Two buckets, never one.** `raw` receives uploads; `derived` holds the generated variants. A single bucket
  would let the worker's own writes retrigger it — **recursive invocation**, an expensive and famous footgun.
- The `derived` bucket is private and readable only by CloudFront, through **Origin Access Control**.
- ECS uses two distinct roles: the **execution role** pulls from ECR and writes logs; the **task role** is what
  application code uses.

## Messaging

- SQS delivery is **at-least-once**, and an S3 event can arrive more than once. The worker is idempotent by
  construction: destination keys are **deterministic** (`derived/{uploadId}/{size}.webp`), so reprocessing
  overwrites the same object with the same bytes.
- Distinguish **transient** from **permanent** failures. A corrupt image or unsupported format is permanent:
  mark the row `FAILED` and **acknowledge** the message. Letting it fail burns the retries and fills the DLQ
  with messages that will never succeed.
- The event source mapping uses **`ReportBatchItemFailures`**. Without it, one bad message in a batch of ten
  sends the whole batch back to the queue.
- The queue's visibility timeout must be **at least six times** the function timeout.
- SQS does **not** do exponential backoff natively. Implementing it requires calling `ChangeMessageVisibility`
  with growing values derived from the message's `ApproximateReceiveCount`.

## Error handling

**Problem Details (RFC 7807)**. Domain exceptions are HTTP-agnostic; a single `GlobalExceptionHandler`
(`@RestControllerAdvice`) maps them to responses using a `ProblemType` enum taxonomy.

## Testing

- Unit tests: `*Test`. Integration tests: `*IntegrationTest` (separate Gradle task).
- Integration tests use **Testcontainers**: `PostgreSQLContainer` for a real PostgreSQL and
  `LocalStackContainer` for S3 and SQS. The whole pipeline runs locally, with no AWS account.
- What LocalStack cannot model faithfully (CloudFront, OAC, IAM, VPC) is covered by a **post-deploy smoke test**
  in the pipeline: real presigned upload, poll until `READY`, then `GET` the CloudFront URL.
- Fixtures use the **Object Mother** pattern (final class, private constructor, static scenario factories, no
  builders).
- Every `@Test` has a Given/When/Then `@DisplayName`.
- Coverage gates: **JaCoCo + Pitest** (thresholds in the root build config).

The three tests that matter most, and that must never be deleted: **idempotency** (same event twice, one row),
**poison message** (corrupt image acknowledged, DLQ stays empty), and **partial batch** (ten messages, one bad,
exactly one `itemIdentifier` returned).

## Infrastructure as code

Terraform, not CDK — the CDK hides AWS resources behind high-level constructs, which is excellent in production
and counterproductive for learning. Every resource is written by hand.

No resource is created through the console, except the one-time bootstrap of the remote state backend.

## Commits — Conventional Commits, strictly

Format: `<type>(<optional scope>): <imperative, lower-case summary>`

Accepted types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`, `perf`, `style`.

Examples:

- `feat(api): issue presigned put url with signed content-type and length`
- `fix(worker): acknowledge poison messages instead of returning them to the queue`
- `refactor(shared): extract s3 event contract into its own record`
- `test(worker): cover partial batch failure response`
- `docs(adr): record why the network has no nat gateway`
- `build(gradle): add localstack testcontainers module to the test classpath`
- `ci(github): authenticate to aws via oidc instead of access keys`
- `chore(infra): tag every resource through provider default_tags`
- `perf(worker): reuse the s3 client across invocations`

### Co-authorship is PROHIBITED

Never add a `Co-Authored-By:` trailer to any commit — not Claude's (`Claude <noreply@anthropic.com>`), not
anyone's. Never add "Generated with Claude Code", a `Claude-Session:` trailer, or any similar marker, to commits,
pull requests, or issues. Authorship of every commit belongs **exclusively to the user**.

## Progress tracking

`docs/PROGRESS.md` holds the MVP roadmap; `docs/FUTURE-PROGRESS.md` holds the post-MVP vision. Both use the same
convention.

When an item is completed:

1. **Check the box and strike the text through**: `- [x] ~~item~~`.
2. **Add bold sub-bullets describing what was actually done** — decisions taken, class and resource names,
   versions, and the reasoning behind them. An item marked done with no sub-bullets does not count as done.
3. Include a **`Verificado:`** sub-bullet with concrete evidence: the command that ran green, the HTTP response
   observed, the resource confirmed in the console or in the Terraform state.
4. When useful, add **`Fora deste item:`** for what was deferred, and **`Caveats:`** for traps discovered along
   the way.

Keep both files in sync with reality as work advances. The sub-bullets are the project's real memory — write them
for a reader who was not there.
