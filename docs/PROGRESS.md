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

- [ ] Monorepo Gradle (Kotlin DSL): `settings.gradle.kts` + `build.gradle.kts` raiz (Java 21, JaCoCo, Pitest), subprojetos `api`, `worker`, `shared`
- [ ] `shared` como **kernel de domínio**: `ImageStatus`, `VariantSize`, `StorageKeys` (derivação das chaves determinísticas). O `build.gradle.kts` dele declara **zero dependências** — a regra que impede o módulo de virar lixeira
- [ ] Esqueleto **hexagonal horizontal** em `api` e `worker`: pacotes `domain`, `application` (`port/in`, `port/out`, `usecase`, `viewmodel`) e `adapter`
- **Critério:** `./gradlew build` verde nos três subprojetos, e o `shared` sem nenhuma dependência declarada.
- **Em paralelo:** é aqui que cabe a passada de largura da teoria, porque esta fase e as duas seguintes não competem por atenção com a AWS. Ver `aws-estudo/aws-roteiro-de-estudo.md`.

## Fase 1 — API local, sem AWS

- [ ] `api/domain`: `Image` e `ImageVariant` (o agregado vive aqui, não no `shared`), mais as exceções de domínio
- [ ] `api/application`: portas `RequestUploadUseCase` e `GetImageUseCase` (in); `ImageRepository`, `PresignedUrlGenerator` e `CdnUrlResolver` (out)
- [ ] `api/adapter`: `web` (controllers, DTOs, `GlobalExceptionHandler`), `persistence`, `storage` (S3), `cdn`
- [ ] `POST /uploads` (valida tipo e tamanho contra allowlist, grava `PENDING`, emite presigned PUT com validade de 5 minutos) e `GET /images/{id}`
- [ ] Assinar `Content-Type` **e** `Content-Length` na presigned URL, para que o cliente não possa enviar outra coisa nem outro tamanho
- [ ] PostgreSQL via Docker Compose + Flyway (`V1__create_images.sql`)
- [ ] Problem Details (RFC 7807) + `GlobalExceptionHandler`
- [ ] Testes: unitários, mais integração com `PostgreSQLContainer` e `LocalStackContainer` (S3)
- **Critério:** a presigned URL emitida contra o LocalStack aceita o `PUT` correto e **recusa** um `Content-Type` divergente.

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
