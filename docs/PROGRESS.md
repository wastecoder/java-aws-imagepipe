# Progresso — imagepipe (MVP)

Roadmap em fases para construir o MVP em cerca de **12 semanas**, a um ritmo de 10 a 15 horas semanais.
A sequência **adia a AWS de propósito**: as Fases **0a, 1 e 2 rodam inteiras na máquina local**, sem conta e
sem gastar um centavo. A conta AWS só nasce na **Fase 0b**, imediatamente antes da Fase 3, para não queimar
o relógio de 6 meses do Free plan enquanto você escreve Java. O brief está em [`PROJECT.md`](PROJECT.md);
a visão pós-MVP em [`FUTURE-PROGRESS.md`](FUTURE-PROGRESS.md).

> **Como usar:** ao concluir um item, marque a caixa **e** risque o texto: `- [x] ~~item concluído~~`.
> Em seguida, **acrescente subitens em negrito descrevendo o que de fato foi feito** — decisões tomadas,
> nomes de classes e recursos, versões, e por quê. Todo item concluído deve ter:
>
> - um subitem **`Verificado:`** com a evidência concreta (comando que rodou verde, resposta HTTP observada,
>   recurso conferido no console ou no state);
> - quando útil, um subitem **`Fora deste item:`** dizendo o que ficou para depois, e **`Caveats:`** para
>   armadilhas descobertas no caminho.
>
> Mantenha este arquivo em dia com a realidade. Um item marcado sem subitens não conta como concluído.

## Objetivos de aprendizado

O projeto existe para ensinar estes pontos. Marque quando você conseguir **explicar o item sem consultar nada**.

- [ ] Presigned URL com `Content-Type` e `Content-Length` assinados (Fase 1)
- [ ] Invocação recursiva do S3 e por que dois buckets a tornam impossível (Fase 4)
- [ ] Entrega at-least-once e idempotência por chave determinística (Fase 2)
- [ ] Erro transitório versus permanente, e por que a DLQ morre sem essa distinção (Fase 2)
- [ ] `ReportBatchItemFailures` e a falha do lote inteiro (Fase 2 e 6)
- [ ] Visibility timeout de pelo menos seis vezes o timeout da função (Fase 6)
- [ ] Backoff exponencial não é nativo da SQS (Fase 10)
- [ ] Gateway Endpoint gratuito versus Interface Endpoint pago versus NAT Gateway (Fase 3 e 10)
- [ ] Lambda dentro da VPC perde o acesso à internet (Fase 6)
- [ ] Autenticação IAM do RDS: token assinado localmente, senha nenhuma (Fase 4)
- [ ] Task role versus execution role no ECS (Fase 5)
- [ ] Security group referenciando security group, não CIDR (Fase 3)
- [ ] CloudFront com OAC mantendo o bucket privado (Fase 7)
- [ ] GitHub Actions com OIDC, sem chave de longa duração no repositório (Fase 9)
- [ ] SSRF e IMDSv2 (fica para o FUTURE, junto da importação por URL)
- [ ] ECS Exec no lugar de bastion com chave SSH (Fase 5)
- [ ] Ciclo de vida do S3 abortando multipart incompleto (Fase 4)
- [ ] `terraform destroy` como instrumento de controle de custo (Fase 3)

## Fase 0a — Fundação do repositório (local, sem AWS)

> Dividido em 22/07/2026. A Fase 0 original criava a conta AWS junto com o repositório, o que fazia o relógio
> de 6 meses do Free plan começar a correr semanas antes de existir qualquer recurso. A parte AWS virou a
> **Fase 0b**, logo antes da Fase 3.

- [x] ~~Monorepo Gradle (Kotlin DSL): `settings.gradle.kts` + `build.gradle.kts` raiz (Java 21, JaCoCo, Pitest), subprojetos `api`, `worker`, `shared`~~
  - **Base gerada pelo Spring Initializr: Spring Boot 4.1.0, Java 21, Gradle 9.5.1, `group = com.wastecoder`, pacote `com.wastecoder.imagepipe`, YAML.** A 3.5.x saiu do site — atingiu EOL em 30/06/2026. Escolhida a 4.1 e não a 4.0.7 porque a 4.0 morre em 31/12/2026, no meio da Fase 7.
  - **Toda versão mora em `gradle/libs.versions.toml`.** Nenhum número em build file.
  - **Config compartilhada no `build.gradle.kts` raiz, via `subprojects {}`** — toolchain 21, baseline de teste, source set `integrationTest`, JaCoCo e Pitest. O plugin do Boot **não** é aplicado na raiz: só o `api` o enxerga.
  - **Gates por camada, não um número global:** `shared` e `domain` 95% linha / 90% branch / 90% mutação; `application` 90/85/85; `adapter` 60/50 e fora do Pitest. Um número único deixaria passar adapter coberto com domínio furado, que é o inverso do que interessa.
  - **`check` = unit + JaCoCo, sem Docker.** `integrationTest` e `pitest` são comandos próprios. O `PROJECT.md` §7 foi corrigido para refletir isso.
  - **Caveats:** `info.solidsoft.pitest` **1.15.0 não funciona no Gradle 9** (`Could not get unknown property 'baseDir'`); a versão boa é a **1.19.0**, que existe só no Plugin Portal — a busca do Maven Central devolve a 1.15.0 como mais recente e engana. O Boot 4.1 gerencia **JUnit Jupiter 6.0.3**, não 5.x, e o Boot 4 renomeou os starters (`spring-boot-starter-webmvc`, um `-test` por slice, `org.testcontainers:testcontainers-postgresql`).
  - **Verificado:** `./gradlew build` verde nos três subprojetos com o Docker desligado; `./gradlew integrationTest` verde com o Docker ligado; `./gradlew pitest` verde.
- [x] ~~`shared` como **kernel de domínio**: `ImageStatus`, `VariantSize`, `StorageKeys` (derivação das chaves determinísticas). O `build.gradle.kts` dele declara **zero dependências** — a regra que impede o módulo de virar lixeira~~
  - **`shared/build.gradle.kts` tem uma linha só, um comentário.** O baseline de teste (JUnit 6.0.3 + AssertJ 3.27.7) é injetado pelo build raiz, então a regra é honrada ao pé da letra e o kernel ainda assim tem teste.
  - **`ImageStatus`:** `PENDING`, `READY`, `FAILED`. **`VariantSize`:** `SMALL` 320px, `MEDIUM` 640px, `LARGE` 1280px, com `maxWidth()` e `slug()` — a largura mora no enum porque é dado de domínio.
  - **`StorageKeys`** com `rawKey(UUID)` → `uploads/{id}`, `derivedKey(UUID, VariantSize)` → `derived/{id}/{slug}.webp`, e o inverso `uploadIdFromRawKey(String)`, de que o worker precisa na Fase 2 para correlacionar o evento do S3 com a linha do banco. A chave `raw` não carrega extensão: o `Content-Type` vai assinado na presigned URL.
  - **O prefixo `derived/` dentro do bucket `derived` é redundante e foi mantido de propósito** — é o que a documentação especifica, e mudar depois seria migração de chave.
  - **Caveat:** o construtor privado de `StorageKeys` precisa ter **corpo vazio**. Com um `throw new AssertionError()` dentro, o JaCoCo deixa de filtrá-lo, a linha descoberta derruba o kernel para 0.94 e o gate de 95% reprova.
  - **Verificado:** `./gradlew :shared:check` verde; `./gradlew :shared:pitest` verde com 100% dos mutantes mortos. O gate foi provado não-vacuoso comentando um teste e vendo o build reprovar com `Rule violated`.
- [x] ~~Esqueleto **hexagonal horizontal** em `api` e `worker`: pacotes `domain`, `application` (`port/in`, `port/out`, `usecase`, `viewmodel`) e `adapter`~~
  - **21 `package-info.java`**, cada um com Javadoc dizendo a responsabilidade da camada. Escolhido em vez de `.gitkeep` porque compila, sobrevive ao Git e serve de âncora para ArchUnit se ele entrar depois.
  - **Granularidade parando no primeiro nível do `adapter`.** `web/dto/request` e `persistence/entity` nascem na Fase 1, junto da primeira classe que mora neles — pacote vazio de terceiro nível é ruído, não mapa.
  - **`ImagepipeApplication` no pacote raiz do `api`**, não em `adapter.config`: é dali que o component scan alcança tudo abaixo.
  - **O teste de contexto do Spring virou `ImagepipeApplicationIntegrationTest`** e foi para `api/src/integrationTest/`, porque sobe um `PostgreSQLContainer`. Deixá-lo em `src/test/` faria o `./gradlew build` exigir Docker.
  - **Verificado:** `grep -rl "springframework\|awssdk\|jakarta.persistence" shared/src worker/src` não retorna nada.
- **Fora deste item:** ArchUnit para transformar a regra do hexágono em gate executável; fat jar do `worker` (Fase 6); pin do `postgres:latest` no `compose.yaml` (Fase 1); `infra/` (Fase 3).
- **Em paralelo:** é aqui que cabe a passada de largura da teoria, porque esta fase e as duas seguintes não competem por atenção com a AWS. Ver `aws-estudo/aws-roteiro-de-estudo.md`.

## Fase 1 — API local, sem AWS

- [x] ~~`api/domain`: `Image` e `ImageVariant` (o agregado vive aqui, não no `shared`), mais as exceções de domínio~~
  - **`Image` e `ImageVariant` escritos à mão, sem Lombok** — a mesma regra da Fase 0a: gerar setters num agregado com invariante seria um buraco direto por ele. `Image` valida `id`, `status`, `contentType`, `sizeBytes > 0` e `createdAt`/`updatedAt` no construtor privado; `pending(...)` e `restore(...)` são as duas fábricas estáticas, a segunda só para reidratar o que veio do banco.
  - **`UploadConstraints` é um `record` com construtor compacto**, não um `@Service` do Spring: a regra "que tipo e que tamanho este deployment aceita" é decisão de domínio, então mora sob o gate de cobertura de domínio (95%/90%) e é testável sem subir contexto nenhum. `UploadConstraintsConfig` só traduz `ImagepipeProperties` nela.
  - **Três exceções HTTP-agnósticas**: `ImageNotFoundException` (carrega o `imageId`), `UnsupportedContentTypeException` (carrega o tipo recusado e a allowlist), `ImageTooLargeException` (carrega o tamanho e o teto). Nenhuma sabe que existe HTTP — quem traduz é o `GlobalExceptionHandler`.
  - **Verificado:** `./gradlew :api:test` verde; `ImageTest` (11 testes), `ImageVariantTest` (6) e `UploadConstraintsTest` (11) cobrindo construção válida, cada `Objects.requireNonNull` e cada `if` de validação.
- [x] ~~`api/application`: portas `RequestUploadUseCase` e `GetImageUseCase` (in); `ImageRepository`, `PresignedUrlGenerator` e `CdnUrlResolver` (out)~~
  - **`RequestUploadUseCaseImpl` recebe um `java.time.Clock` no construtor**, não chama `Instant.now()` direto — é o que permite `RequestUploadUseCaseImplTest` fixar `createdAt` com `Clock.fixed(...)` em vez de comparar contra um instante variável. `UseCaseConfig` expõe `Clock.systemUTC()` como bean.
  - **Ordem deliberada dentro do use case**: valida (`UploadConstraints.validate`) antes de gerar qualquer efeito colateral, grava a linha `PENDING` antes de assinar a URL. Se a assinatura falhar depois do `save`, sobra uma linha `PENDING` órfã — aceitável; se fosse ao contrário, o cliente teria uma credencial de upload para uma linha que não existe.
  - **`CdnUrlResolver` já se chama assim, não `LocalStackUrlResolver`**: a Fase 7 troca a implementação (`CloudFrontUrlResolver`) de propriedade, não de nome de porta.
  - **Verificado:** `RequestUploadUseCaseImplTest` (5 testes) e `GetImageUseCaseImplTest` (3) verdes, mockando as portas `out` com Mockito.
- [x] ~~`api/adapter`: `web` (controllers, DTOs, `GlobalExceptionHandler`), `persistence`, `storage` (S3), `cdn`~~
  - **Lombok aparece só em `ImageEntity`** (`@Getter @Setter @NoArgsConstructor`): é a única classe do projeto que o gate de cobertura já exclui por regra (JPA exige construtor vazio e setters), então gerar esse código não fura métrica nenhuma. O agregado (`Image`) continua escrito à mão.
  - **`image_variants` mapeada como `@ElementCollection(fetch = FetchType.EAGER)`**: no máximo três variantes por imagem, e `GET /images/{id}` sempre precisa delas — `LAZY` só compraria `LazyInitializationException` fora da transação.
  - **`S3Presigner.Builder` não tem `forcePathStyle`**; esse método só existe em `S3ClientBuilder`. Sem `serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true))` a URL sai como `http://<bucket>.localhost:4566/...`, um host que não resolve.
  - **Decisão:** `domain` e `application` sem nenhuma anotação de Spring; a composição inteira mora em `adapter.config` (`UseCaseConfig`, `UploadConstraintsConfig`, `AwsS3Config`, `CdnConfig`) — é o que mantém as camadas de dentro testáveis com `new` puro.
  - **Swagger UI só no profile `local`**, em `/swagger-ui.html` (`/v3/api-docs` para o spec), com o exemplo de `curl.exe` embutido na própria descrição do endpoint `POST /uploads`: o Swagger não consegue fazer o passo 2 (o `PUT` vai para o S3, não para esta API).
  - **Verificado:** `grep` não encontra `springframework` nem `awssdk` em `api/src/main/java/.../domain`; `S3PresignedUrlGeneratorTest` (4) e `CloudFrontUrlResolverTest` (4) verdes com dublês.
- [x] ~~`POST /uploads` (valida tipo e tamanho contra allowlist, grava `PENDING`, emite presigned PUT com validade de 5 minutos) e `GET /images/{id}`~~
  - **Formato de resposta do `POST /uploads`**: `201`, header `Location: /images/{id}`, corpo `{uploadId, status, upload:{method, url, requiredHeaders, expiresAt}}` — as instruções de upload aninhadas sob `upload` separam o recurso durável do credential efêmero, e deixam espaço para a Fase 19 trocar por uma presigned POST sem mudar o formato de fora.
  - **`GET /images/{id}`**: `200` com `{id, status, contentType, sizeBytes, filename, createdAt, variants}`; `variants` fica vazio até a Fase 2 existir — não é bug, é o estado real do sistema hoje.
  - **Verificado (passeio manual via `curl`, com a API rodando por `./gradlew.bat :api:bootRun` e o Docker já de pé):**
    `POST /uploads` com `{"contentType":"image/png","sizeBytes":21,"filename":"cat.png"}` devolveu `201` com `Location: /images/{uuid}` e `upload.requiredHeaders = {"Content-Length":"21","Content-Type":"image/png"}`.
    `GET /images/{uuid}` devolveu `200` com `{"status":"PENDING", ..., "variants":[]}`.
    `POST /uploads` com `{"contentType":"application/zip", ...}` devolveu `422` `application/problem+json` com `type=.../problems/unsupported-content-type` e a allowlist no corpo.
- [x] ~~Assinar `Content-Type` **e** `Content-Length` na presigned URL, para que o cliente não possa enviar outra coisa nem outro tamanho~~
  - **`Content-Length` é assinado de verdade pelo SDK Java v2** — medido contra o LocalStack com validação de assinatura ligada: `X-Amz-SignedHeaders` sai como `content-length;content-type;host`. Fecha o objetivo de aprendizado nº 1 da lista no topo deste arquivo.
  - **`S3PresignedUrlGenerator.requiredHeaders(...)` deriva o mapa devolvido ao cliente a partir de `presigned.signedHeaders()`**, em vez de reconstruí-lo à mão — assim o contrato documentado nunca pode divergir da assinatura real se um campo novo entrar no `PutObjectRequest` depois.
  - **Caveat:** presigned PUT trava tamanho **exato**, não teto. Faixa só existe na condição `content-length-range` da POST policy, que o SDK Java v2 não gera (issue #1493 em aws/aws-sdk-java-v2). **A confirmação contra o S3 real fica para a Fase 5/6.**
  - **Caveat:** `Content-Type` é metadado, não validação — o S3 não inspeciona o corpo do arquivo. O worker da Fase 2 precisa checar magic bytes e tratar divergência como `PermanentProcessingException`.
  - **Verificado:** `PUT` com `Content-Type: image/png` sobre os 21 bytes assinados devolveu `200`; o mesmo `PUT` com `Content-Type: image/jpeg` devolveu `403 SignatureDoesNotMatch`, tanto em `UploadFlowIntegrationTest.refusesADivergentContentType` quanto à mão via `curl -T` contra a API local. Igual para um corpo mais curto que o assinado (`refusesADivergentContentLength`).
- [x] ~~PostgreSQL via Docker Compose + Flyway (`V1__create_images.sql`)~~
  - **`image_variants` com PK natural `(image_id, size)`**, mapeada como `@ElementCollection` — é ela que torna o upsert da Fase 2 idempotente por construção: `INSERT ... ON CONFLICT (image_id, size) DO UPDATE` não pode duplicar uma variante, mesmo que o mesmo evento S3 chegue duas vezes.
  - **Caveat:** `./gradlew :api:bootRun` roda com working dir em `api/` por padrão, e o `compose.yaml` da raiz não é encontrado sem `workingDir = rootProject.projectDir` na configuração do `BootRun` (ver `api/build.gradle.kts`). Sem isso o `spring-boot-docker-compose` sobe silenciosamente nada.
  - **`compose.yaml` publica as duas portas fixas (`5432`, `4566`)** — o LocalStack não tem connection details factory no Spring Boot, então uma porta efêmera não poderia ser lida por propriedade nenhuma.
  - **Verificado:** `docker compose up -d` seguido de `./gradlew.bat :api:bootRun` aplicou `V1__create_images.sql` num schema vazio (log do Flyway: `Successfully applied 1 migration to schema "public", now at version v1`) contra o Postgres do compose.
- [x] ~~Problem Details (RFC 7807) + `GlobalExceptionHandler`~~
  - **`ProblemType`** enumera os quatro tipos hoje existentes, todos sob `https://imagepipe.wastecoder.com/problems/`: `invalid-payload` (400, bean validation), `unsupported-content-type` (**422**), `image-too-large` (**422**), `image-not-found` (404). Os dois 422 usam o mesmo status HTTP de propósito — são distinguidos pelo `type`, que é exatamente para isso que o Problem Details serve.
  - **`GlobalExceptionHandler` estende `ResponseEntityExceptionHandler`** em vez de só usar `@ExceptionHandler` solto: isso desliga o handler default do Boot (que é `@ConditionalOnMissingBean` desse tipo), então `spring.mvc.problemdetails.enabled` nunca precisou ser setado em lugar nenhum.
  - **Caveat:** todo hook de `ResponseEntityExceptionHandler` é anulável no Framework 7 — devolve `null` quando a resposta já foi committada. `handleMethodArgumentNotValid` (usado para 400 de bean validation) checa isso antes de mexer no corpo.
  - **Verificado:** `UploadControllerTest` (5 testes) e `ImageControllerTest` (3) cobrindo os quatro tipos de problema fim a fim via `@WebMvcTest`.
- [x] ~~Testes: unitários, mais integração com `PostgreSQLContainer` e `LocalStackContainer` (S3)~~
  - **A imagem do LocalStack está pinada em `4.14.0`**, o último release antes de a imagem unificada passar a exigir `LOCALSTACK_AUTH_TOKEN` (23/03/2026); o escape `LOCALSTACK_ACKNOWLEDGE_ACCOUNT_REQUIREMENT` expirou em 06/04/2026. Sem o pino, `latest` quebra o ambiente local sem aviso.
  - **`S3_SKIP_SIGNATURE_VALIDATION=0` não é opcional.** O padrão do LocalStack é `1`, e com ele um `PUT` com `Content-Type` errado, `Content-Length` errado ou URL expirada devolve `200` e só loga um aviso — toda asserção sobre presigning passaria sem provar nada. Com a validação ligada, a secret key precisa ser literalmente `test`; qualquer outra falha de um jeito que parece bug de assinatura. `LocalStackPresignContractIntegrationTest` é o teste-guarda que prova que o ambiente de fato valida.
  - **`UploadFlowIntegrationTest` é o teste que amarra a fase inteira**: `MockMvc` dirigindo a API e um `HttpClient` de verdade dirigindo o `PUT` — deliberado, porque o `PUT` não passa por esta aplicação, esse é o ponto inteiro de uma presigned URL. `Content-Length` não pode ser setado à mão (`java.net.http` recusa como header restrito); o caso de corpo divergente é produzido mandando um corpo mais curto que o assinado.
  - **Boot 4 removeu `@MockBean`** (→ `@MockitoBean`) **e moveu `@WebMvcTest`** para `org.springframework.boot.webmvc.test.autoconfigure`. **`@DynamicPropertySource` é ignorado em métodos `@Bean`** dentro de uma `@TestConfiguration` — usa-se `DynamicPropertyRegistrar` em vez disso (ver `TestcontainersConfiguration`).
  - **`checksumValidationEnabled(false)` foi removido de `LocalStackPresignContractIntegrationTest`** (era o último uso no `integrationTest` source set; já tinha saído do `AwsS3Config` de produção antes). O método está deprecado no SDK, o javadoc do `S3Presigner` diz que a opção nem se aplica a presigned requests, e a medição contra o SDK 2.49.2 não achou diferença de header nenhuma com ou sem ele — `S3PresignedUrlGeneratorIntegrationTest` continua exigindo exatamente `Content-Type` e `Content-Length` em `requiredHeaders` e continua verde.
  - **Caveat — a não-vacuidade do Step 6 não se sustentou.** A tentativa original era comentar `ImageTest.startsPendingWithoutVariants` e esperar `./gradlew.bat :api:check` reprovar com `Rule violated for package com.wastecoder.imagepipe.api.domain.model`. Rodado de verdade: `BUILD SUCCESSFUL`, gate não reprovou. Investigando: o pacote `domain.model` tem 51 linhas / 18 branches ao todo (95%/90% de corte), e **cada campo do agregado é lido de novo por pelo menos um teste de camada acima** (`ImageEntityMapperTest`, `GetImageUseCaseImplTest`, `ImageControllerTest`, `RequestUploadUseCaseImplTest`), porque essas suítes exercitam o objeto de domínio de verdade em vez de mockar `Image`/`ImageVariant`/`UploadConstraints`. Cada `if` de validação (`sizeBytes<=0`, `width<=0`, etc.) também tem só um teste dedicado cobrindo o ramo "true", e derrubar 1 branch de 18 (94,4%) ou 1-2 linhas de 51 não cruza nenhum dos dois cortes — precisaria de **2+ branches** ou **3+ linhas** simultâneos. Testei empiricamente também `ImageNotFoundExceptionTest.keepsTheImageId` (pacote `domain.exception`, corte mais apertado: 16 linhas, 1 linha perdida já reprova) — mesmo resultado, `BUILD SUCCESSFUL`, porque `ImageControllerTest.returnsNotFound` e `GetImageUseCaseImplTest.reportsAMissingImage` já constroem/lançam a exceção real e chamam `imageId()`. **Não encontrei nenhum teste único, hoje, cuja remoção reprove o gate do JaCoCo em `domain.model` ou `domain.exception`** — é um efeito colateral (bom, no fundo) de testar as camadas de cima contra objetos de domínio reais em vez de dublês. Isto é a terceira vez que um passo de não-vacuidade planejado não se sustenta neste projeto (as outras duas: Fase 0a e a Tarefa 2 desta fase) — fica registrado para quem planejar o próximo: valide a não-vacuidade *antes* de escrever o passo, rodando o comando de verdade.
  - **O gate É não-vacuoso; o instrumento é que estava errado.** Verificado depois, movendo o arquivo `UploadConstraintsTest.java` inteiro para fora do source set e rodando `./gradlew.bat clean :api:check`: `Rule violated for package com.wastecoder.imagepipe.api.domain.model: lines covered ratio is 0.94, but expected minimum is 0.95` e `branches covered ratio is 0.83, but expected minimum is 0.90`. Arquivo restaurado, `git status` limpo. A regra geral que sai daí: **com o padrão Object Mother, remover *um* teste não prova nada**, porque a `ImageMother` exercita as mesmas linhas de produção em nome de todas as suítes que a usam, e as camadas de cima testam contra objetos de domínio reais. A sonda certa é remover uma **classe de teste inteira** — de preferência a única que cobre um tipo (aqui, `UploadConstraints`). Vale para as próximas fases: o `worker` vai ter Object Mothers também.
  - **Verificado:** `./gradlew.bat clean build integrationTest pitest` verde numa única invocação (`BUILD SUCCESSFUL in 1m 26s`, 25 tasks, 24 executadas e 1 up-to-date — `clean` na frente garante que nada ficou em cache). `:api:integrationTest` rodou de verdade (não `UP-TO-DATE`): 20 testes, 0 falhas — 1 `contextLoads`, 6 do contrato do LocalStack, 3 do repositório, 4 do gerador de presigned URL, 6 do `UploadFlowIntegrationTest`. `:api:pitest`: 28/28 mutantes mortos (100%); `:shared:pitest`: 6/6 (100%).
- **Fora deste item:** worker e transcodificação (Fase 2); presigned POST (Fase 19); CloudFront real (Fase 7); ADRs numerados (Fase 11).
- **Critério:** a presigned URL emitida contra o LocalStack aceita o `PUT` correto e **recusa** um `Content-Type` divergente. **Atendido** — ver `UploadFlowIntegrationTest.refusesADivergentContentType` e a evidência de `curl` acima.
- **Caveat:** `POST /uploads` está sem autenticação, e nenhuma fase deste arquivo adiciona uma — o Cognito só aparece em `FUTURE-PROGRESS.md`. Localmente isso é irrelevante; atrás do ALB público que a Fase 5 liga, é uma máquina de emitir presigned URLs aberta ao mundo: qualquer um pode gerar credenciais de escrita no S3 sem limite e linhas `PENDING` sem limite. Todo controle que esta fase tem (tamanho exato, tipo exato, TTL de 5 minutos) é por requisição — nenhum deles limita o volume de requisições. É um estado aceito de propósito num ambiente de aprendizado, mas a Fase 5 precisa nomear essa lacuna explicitamente ao expor o ALB, não descobri-la depois.

## Fase 2 — Worker local, sem AWS

- [ ] `worker/domain`: `TransientProcessingException` e `PermanentProcessingException` como conceitos de **domínio**, não detalhes de infraestrutura
- [ ] `worker/application`: `ProcessUploadUseCase`, com as portas de saída `ObjectStore`, `ImageStatusRepository` e `ImageTranscoder`
- [ ] `worker/adapter`: `lambda` (`SqsEventHandler`, um `RequestHandler<SQSEvent, SQSBatchResponse>` em **Java puro, sem Spring**), `storage` (S3), `persistence` (JDBC), `transcoder`
- [ ] O adaptador Lambda é o **único** lugar que traduz as duas exceções de domínio em "devolve para a fila" ou "reconhece a mensagem"
- [ ] Geração das variantes com **chaves determinísticas** (`derived/{uploadId}/{tamanho}.webp`, derivadas por `StorageKeys` no `shared`), o que torna o trabalho idempotente por construção
- [ ] Atualização do PostgreSQL via JDBC, com upsert
- [ ] Teste de **idempotência**: o mesmo evento processado duas vezes produz o mesmo resultado, sem linha duplicada
- [ ] Teste de **mensagem venenosa**: imagem corrompida vira `FAILED` e a mensagem é reconhecida, não devolvida
- [ ] Teste de **lote parcial**: lote de 10 com 1 ruim devolve exatamente um `itemIdentifier`
- **Critério:** o pipeline inteiro roda na máquina, do `PUT` ao status `READY`, sem conta na AWS.

## Fase 0b — Fundação da conta AWS (antes da Fase 3)

> A outra metade da Fase 0 original. **Só execute depois da Fase 2.** A partir daqui o relógio de 6 meses do
> Free plan começa a correr, e nada nas fases anteriores precisa de conta. Contexto em
> `aws-estudo/guia-aws-pedro-2026.md`, seção 5.2.

- [ ] Conta AWS no **Free plan**: MFA no usuário root, root guardado, usuário IAM administrador separado para o dia a dia
- [ ] **AWS Budgets** com alertas em US$ 1, US$ 5 e US$ 10, mais billing alarm no CloudWatch (esta atividade rende US$ 20 de crédito)
- [ ] Backend remoto do Terraform: bucket S3 versionado, com lock via `use_lockfile` (Terraform 1.10+) ou tabela DynamoDB (padrão antigo)
- **Critério:** um `terraform apply` de recurso trivial funciona com state remoto, e o alarme de custo está ativo.

## Fase 3 — Rede em Terraform

- [ ] VPC em duas zonas de disponibilidade; subnets públicas e privadas; internet gateway; route tables
- [ ] Security groups: `alb-sg`; `api-sg` aceitando ingress **apenas de `alb-sg`**; `lambda-sg`; `rds-sg` aceitando ingress apenas de `api-sg` e `lambda-sg` — **referenciando security group, nunca CIDR**
- [ ] **VPC Gateway Endpoint para S3** (gratuito). Nenhum NAT Gateway em lugar nenhum
- [ ] Aplicar e destruir o stack ao menos cinco vezes, até o ciclo virar reflexo
- **Critério:** `terraform destroy` deixa zero recurso faturável, e `grep nat_gateway` no state não retorna nada.

## Fase 4 — Dados: S3 e RDS

- [ ] Buckets `raw` e `derived`: `BlockPublicAccess` total, criptografia em repouso, versionamento
- [ ] Regra de ciclo de vida abortando **multipart upload incompleto** após 7 dias
- [ ] RDS PostgreSQL `db.t4g.micro` em subnet privada, sem acesso público, com **autenticação IAM habilitada**
- [ ] Confirmar que **nenhuma senha** existe: nem no state do Terraform, nem em variável de ambiente
- **Critério:** o RDS está de pé com IAM auth ligado. A verificação de conectividade real e as migrações ficam para a Fase 5, quando existir um compute dentro da VPC.

## Fase 5 — Compute: ECR, Fargate e ALB

- [ ] Dockerfile multi-stage (JRE slim, usuário não-root) e push da imagem para o **ECR**
- [ ] Task definition com **execution role** (puxa do ECR, escreve logs) e **task role** (`s3:PutObject` em `uploads/*`, `rds-db:connect`) — deliberadamente distintas
- [ ] Serviço ECS Fargate em subnet pública; ALB com target group e health check em `/actuator/health`
- [ ] Domínio registrado, hosted zone no Route 53, certificado do ACM e HTTPS no ALB
- [ ] **ECS Exec** habilitado, para abrir shell na task sem bastion e sem chave SSH; migrações Flyway aplicadas no RDS
- **Critério:** `https://<dominio>/actuator/health` responde 200, e o `POST /uploads` grava `PENDING` no RDS usando token IAM.

## Fase 6 — Assíncrono: SQS e Lambda

- [ ] Notificação de evento do bucket `raw` para a fila SQS
- [ ] Fila principal e **DLQ** com redrive policy (`maxReceiveCount` 5); visibility timeout de **pelo menos 6x** o timeout da função
- [ ] Lambda em subnet privada, com execution role mínima (`s3:GetObject` em `raw/*`, `s3:PutObject` em `derived/*`, `rds-db:connect`)
- [ ] Event source mapping com **`ReportBatchItemFailures`** habilitado
- [ ] Confirmar que a Lambda alcança o S3 pelo **Gateway Endpoint** e o RDS por token IAM, sem NAT e sem Interface Endpoint
- **Critério:** um upload real gera as variantes; um evento duplicado não duplica linha; uma imagem corrompida vai para `FAILED` sem entupir a DLQ.

## Fase 7 — CDN: CloudFront e OAC

- [ ] Distribuição CloudFront com **Origin Access Control** sobre o bucket `derived`
- [ ] Bucket policy restringindo a leitura ao ARN da distribuição
- [ ] Política de cache e procedimento de invalidação
- [ ] `GET /images/{id}` passa a devolver as URLs do CloudFront
- **Critério:** a URL direta do S3 retorna **403** e a URL do CloudFront retorna **200** com o content-type correto.

## Fase 8 — Observabilidade

- [ ] Log estruturado em JSON na `api` e no `worker`
- [ ] Métrica customizada: variantes geradas e tempo de processamento
- [ ] Alarmes: profundidade da DLQ maior que zero, 5xx no ALB, erro do worker
- [ ] Dashboard no CloudWatch; tracing com X-Ray na `api` e no `worker`
- **Critério:** uma mensagem chegando na DLQ dispara o alarme, e o trace mostra o caminho do upload ao `READY`.

## Fase 9 — CI/CD com OIDC

- [ ] Role IAM assumida pelo GitHub Actions via **OIDC** — nenhuma chave de longa duração no repositório
- [ ] Pull request: build, testes unitários, testes de integração (Testcontainers), `terraform validate`, `tflint`, `tfsec`, e `terraform plan` comentado no PR
- [ ] `main`: build da imagem, push no ECR, `terraform apply`, deploy do serviço ECS
- [ ] **Smoke test pós-deploy**: upload real por presigned URL, polling até `READY`, `GET` na URL do CloudFront
- **Critério:** pipeline verde ponta a ponta, e os secrets do repositório não contêm `AWS_ACCESS_KEY_ID`.

## Fase 10 — Rigor de mensageria

- [ ] Backoff exponencial manual, via `ChangeMessageVisibility` com valores crescentes baseados no `ApproximateReceiveCount` da mensagem
- [ ] **ADR** documentando o trade-off descoberto: chamar a API da SQS de dentro da VPC exige um Interface Endpoint pago (cerca de US$ 7/mês por AZ), enquanto o visibility timeout fixo é gratuito
- [ ] Replay da DLQ (redrive) por comando ou endpoint administrativo
- [ ] Teste de carga leve, validando o comportamento sob concorrência
- **Critério:** uma mensagem que falha repetidamente demonstra intervalos crescentes até cair na DLQ.

## Fase 11 — Agendamento e documentação

- [ ] **EventBridge Scheduler** diário: apaga uploads órfãos (`PENDING` vencido) e objetos em `raw` sem par em `derived`
- [ ] Regras de ciclo de vida movendo os originais para uma classe de armazenamento fria
- [ ] `docs/`: `ARCHITECTURE.md`, `API.md`, `DEVELOPMENT.md`, `TESTS.md`, `COSTS.md` e ADRs numerados em `docs/adr/`
- [ ] `README.md` em português, com o diagrama e o **relatório real do Cost Explorer** do trimestre
- **Critério:** o custo real está documentado, `docs/` está navegável com índice, e os 18 objetivos de aprendizado estão marcados.
