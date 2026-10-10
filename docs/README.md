# ImagePipe

Pipeline de processamento de imagens orientado a eventos, com upload via URL pré-assinada,
geração assíncrona de variantes e distribuição via CDN.
> **Status:** Em desenvolvimento. O projeto ainda não está concluído. Acompanhe o progresso e as próximas etapas em [`docs/PROGRESS.md`](PROGRESS.md).

---

## Tecnologias

- **Linguagem:** Java
- **Backend:** Spring Boot
- **Armazenamento:** Amazon S3
- **Mensageria:** Amazon SQS
- **Processamento assíncrono:** AWS Lambda
- **Execução da API:** Amazon ECS Fargate
- **Banco de dados:** PostgreSQL (Amazon RDS)
- **CDN:** Amazon CloudFront
- **Infraestrutura como código:** Terraform

---

## Arquitetura

1. A API gera uma URL pré-assinada para upload direto ao S3.
2. O upload dispara um evento que é encaminhado ao SQS.
3. Uma função Lambda consome o evento e gera variantes da imagem.
4. As variantes são armazenadas em um bucket S3 separado, e os metadados são atualizados no PostgreSQL.
5. As imagens processadas são disponibilizadas pelo CloudFront.

---

## Execução local

O ambiente local utiliza Docker Compose e LocalStack para simular os serviços AWS necessários ao desenvolvimento.

```bash
docker compose up -d
./gradlew build
./gradlew :api:bootRun
```

---

## Testes
```bash
./gradlew test
./gradlew integrationTest
./gradlew check
```

---

# Objetivo
Projeto de estudo e portfólio focado em arquitetura orientada a eventos, processamento assíncrono,
infraestrutura como código e aprofundamento nos principais serviços da AWS.
