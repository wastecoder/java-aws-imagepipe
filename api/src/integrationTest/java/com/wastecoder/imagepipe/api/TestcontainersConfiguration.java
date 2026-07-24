package com.wastecoder.imagepipe.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// Public, unlike the version phase 0a left behind: integration tests in adapter subpackages import
// it and read the bucket constants, and package-private would not reach them.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String RAW_BUCKET = "imagepipe-raw";
    public static final String DERIVED_BUCKET = "imagepipe-derived";

    /** Pinned to the same tag as compose.yaml. `latest` would roll to PostgreSQL 19 on its own. */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    }

    /**
     * Pinned to 4.14.0, the last release before the unified image started demanding
     * LOCALSTACK_AUTH_TOKEN, and started with signature validation on. See
     * LocalStackPresignContractIntegrationTest for why that flag is not optional.
     */
    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
                .withServices("s3")
                .withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0");
    }

    /**
     * Spring Boot has no connection details factory for LocalStack, so the properties are registered
     * by hand. It has to be a DynamicPropertyRegistrar bean and not a @DynamicPropertySource method:
     * the latter must be static and declared on the test class, and is silently ignored here.
     */
    @Bean
    DynamicPropertyRegistrar localStackProperties(LocalStackContainer localstack) {
        return registry -> {
            registry.add("imagepipe.aws.endpoint", () -> localstack.getEndpoint().toString());
            registry.add("imagepipe.aws.region", localstack::getRegion);
            registry.add("imagepipe.aws.access-key", localstack::getAccessKey);
            registry.add("imagepipe.aws.secret-key", localstack::getSecretKey);
            registry.add("imagepipe.cdn.base-url", () -> localstack.getEndpoint() + "/" + DERIVED_BUCKET);
        };
    }

    /**
     * Test-only client, used to assert what actually landed in the bucket. Creating the buckets here
     * is a deliberate side effect: it is the one place that runs exactly once per application
     * context, and the application expects them to exist.
     */
    @Bean
    S3Client s3Client(LocalStackContainer localstack) {
        S3Client client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .forcePathStyle(true)
                .build();

        client.createBucket(request -> request.bucket(RAW_BUCKET));
        client.createBucket(request -> request.bucket(DERIVED_BUCKET));
        return client;
    }
}
