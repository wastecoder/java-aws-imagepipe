# Próximos passos — Evolução futura

Ideias para evoluir o `imagepipe` **depois** do MVP. Mesmo formato do [`PROGRESS.md`](PROGRESS.md): ao concluir,
marque a caixa, risque o texto e acrescente os subitens em negrito descrevendo o que foi feito, com um subitem
`Verificado:`. **Nada aqui é necessário para o portfólio inicial.**

Cada fase abaixo tem um custo associado. Antes de puxar qualquer uma para o MVP, confirme o saldo de créditos.

## Fase 12 — Identidade gerenciada

- [ ] Substituir o JWT próprio por **Amazon Cognito** (user pool, grupos, escopos)
- [ ] Autorização por escopo no ALB ou na aplicação
- **Critério:** o login deixa de ser responsabilidade da aplicação, e o token é emitido e validado pelo Cognito.

## Fase 13 — Alternativas de armazenamento de dados

- [ ] **DynamoDB** para a tabela de idempotência, com escrita condicional (`attribute_not_exists`)
- [ ] **Aurora Serverless v2** com scale-to-zero no lugar do RDS, e comparação de custo entre os dois
- [ ] ADR comparando modelo relacional e chave-valor para este domínio
- **Critério:** o ambiente ocioso passa a custar apenas armazenamento.

## Fase 14 — API serverless

- [ ] Migrar a `api` para **Lambda com AWS Lambda Web Adapter**, empacotando o Spring Boot sem reescrever para o modelo de handler
- [ ] Habilitar **SnapStart** e medir o cold start antes e depois
- [ ] ADR comparando Fargate e Lambda: custo ocioso, cold start, complexidade operacional
- **Critério:** o ambiente inteiro roda dentro das cotas sempre gratuitas quando não há tráfego.

## Fase 15 — Orquestração de pipeline

- [ ] **Step Functions** coordenando um pipeline de várias etapas: validar, moderar, gerar variantes, publicar
- [ ] **EventBridge** como barramento entre as etapas, no lugar da fila única
- **Critério:** o fluxo é visível como máquina de estados, com retentativa e compensação por etapa.

## Fase 16 — Segurança avançada

- [ ] **AWS WAF** na frente do CloudFront (rate limit, regras gerenciadas)
- [ ] **KMS com chave gerenciada pelo cliente** no lugar da criptografia padrão do S3
- [ ] Importação de imagem **por URL**, com defesa contra **SSRF**: allowlist de destino, bloqueio de faixas privadas e do endpoint de metadados, e **IMDSv2** obrigatório
- [ ] Varredura de imagem de container (ECR scan on push) e política de bloqueio no pipeline
- **Critério:** um teste automatizado tenta buscar `169.254.169.254` e é bloqueado.

## Fase 17 — Entrega progressiva

- [ ] Deploy **blue/green** no ECS com CodeDeploy, com rollback automático em alarme
- [ ] Múltiplos ambientes (dev e prod) com **Terraform workspaces** ou diretórios separados
- **Critério:** rollout e rollback sem downtime, disparados pelo pipeline.

## Fase 18 — Escala e performance

- [ ] Auto scaling do serviço ECS por métrica de profundidade da fila
- [ ] **Fargate Spot** para o processamento tolerante a interrupção
- [ ] Teste de carga com k6, medindo latência do upload e vazão do worker
- **Critério:** a fila drena sob carga sem intervenção manual, e o custo por imagem processada é conhecido.

## Fase 19 — Formatos e qualidade de imagem

- [ ] Suporte a **AVIF** e **HEIC** (TwelveMonkeys ImageIO)
- [ ] Trocar a presigned PUT por **presigned POST** com política `content-length-range`, para o próprio S3 rejeitar arquivos grandes
- [ ] Moderação de conteúdo com **Amazon Rekognition** (atenção: serviço pago, fora das cotas gratuitas)
- **Critério:** um arquivo acima do limite é recusado pelo S3, sem chegar à aplicação nem ao worker.

## Fase 20 — FinOps

- [ ] Tags de custo em todos os recursos, aplicadas por `default_tags` no provider
- [ ] **Cost Anomaly Detection** e relatório mensal por tag no Cost Explorer
- [ ] Estimativa de custo no pull request com Infracost
- **Critério:** cada pull request mostra o delta de custo que a mudança de infraestrutura provoca.

## Fase 21 — Multi-região

- [ ] Replicação entre regiões do bucket `derived`
- [ ] Failover de leitura e medição de latência a partir do Brasil
- [ ] ADR sobre o custo real da redundância geográfica
- **Critério:** a perda de uma região não interrompe a entrega das imagens.

## Fase 22 — Kubernetes

- [ ] **EKS** no lugar do ECS, com Helm; HPA; probes de liveness e readiness
- [ ] ADR comparando ECS e EKS para uma carga deste tamanho
- **Critério:** o mesmo sistema sobe com `helm install`. Atenção: o plano de controle do EKS tem custo fixo por hora, incompatível com o orçamento do MVP.
