package com.wastecoder.imagepipe.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Proves that the LocalStack container this project uses actually verifies presigned signatures.
 *
 * <p>It exists because the default is the opposite: with {@code S3_SKIP_SIGNATURE_VALIDATION=1},
 * which is what LocalStack ships, a PUT carrying the wrong Content-Type, the wrong Content-Length or
 * an expired signature all return 200 and only log a warning. Every later assertion about presigning
 * would then pass while proving nothing. This test is the guard on that guard.
 *
 * <p>It uses the AWS SDK directly rather than the application's adapter on purpose: the subject here
 * is the environment, not our code.
 */
@Testcontainers
class LocalStackPresignContractIntegrationTest {

    private static final String BUCKET = "presign-contract";
    private static final byte[] BODY = "the exact bytes that were signed for".getBytes(StandardCharsets.UTF_8);

    @Container
    private static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
                    .withServices("s3")
                    .withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0");

    private static S3Client s3;
    private static S3Presigner presigner;

    @BeforeAll
    static void startClients() {
        // The secret key must be exactly "test": with validation enabled LocalStack can only match a
        // signature produced with that key, and any other value fails every request in a way that
        // looks like a bug in the signing code.
        AwsCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));

        s3 = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credentials)
                .forcePathStyle(true)
                .build();
        s3.createBucket(request -> request.bucket(BUCKET));

        presigner = S3Presigner.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credentials)
                // S3Presigner.Builder has no forcePathStyle; that method only exists on
                // S3ClientBuilder. Without path style the URL is built as
                // http://<bucket>.localhost:<port>/..., a host that does not resolve.
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .checksumValidationEnabled(false)
                        .build())
                .build();
    }

    @AfterAll
    static void closeClients() {
        s3.close();
        presigner.close();
    }

    @Test
    @DisplayName("Given a put object request with content type and length, when presigning, then both are signed")
    void signsContentTypeAndContentLength() {
        PresignedPutObjectRequest presigned =
                presign("signed-headers", "image/png", BODY.length, Duration.ofMinutes(5));

        assertThat(presigned.signedHeaders()).containsOnlyKeys("host", "content-type", "content-length");
        assertThat(presigned.signedHeaders().get("content-type")).containsExactly("image/png");
        assertThat(presigned.signedHeaders().get("content-length"))
                .containsExactly(String.valueOf(BODY.length));
    }

    @Test
    @DisplayName("Given a presigned url, when putting with the signed headers, then the object is stored")
    void acceptsTheSignedRequest() throws Exception {
        String key = "happy-path";

        HttpResponse<String> response =
                put(presign(key, "image/png", BODY.length, Duration.ofMinutes(5)), BODY, "image/png");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(s3.headObject(request -> request.bucket(BUCKET).key(key)).contentLength())
                .isEqualTo(BODY.length);
    }

    @Test
    @DisplayName("Given a presigned url, when putting with a different content type, then it is refused")
    void refusesADivergentContentType() throws Exception {
        HttpResponse<String> response =
                put(presign("wrong-type", "image/png", BODY.length, Duration.ofMinutes(5)), BODY, "image/jpeg");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given a presigned url, when putting without the content type, then it is refused")
    void refusesAMissingContentType() throws Exception {
        HttpResponse<String> response =
                put(presign("no-type", "image/png", BODY.length, Duration.ofMinutes(5)), BODY, null);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given a presigned url, when putting a different number of bytes, then it is refused")
    void refusesADivergentContentLength() throws Exception {
        // Content-Length cannot be set by hand: java.net.http rejects it as a restricted header name.
        // The client derives it from the body, so a shorter body is how the mismatch is produced.
        byte[] shorter = "too short".getBytes(StandardCharsets.UTF_8);

        HttpResponse<String> response =
                put(presign("wrong-length", "image/png", BODY.length, Duration.ofMinutes(5)), shorter, "image/png");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given an expired presigned url, when putting with the signed headers, then it is refused")
    void refusesAnExpiredUrl() throws Exception {
        PresignedPutObjectRequest presigned =
                presign("expired", "image/png", BODY.length, Duration.ofSeconds(1));
        // The only sleep in the whole suite. X-Amz-Date is stamped at signing time, so there is no
        // way to observe expiry other than letting it pass.
        Thread.sleep(1_500);

        HttpResponse<String> response = put(presigned, BODY, "image/png");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("Request has expired");
    }

    private static PresignedPutObjectRequest presign(
            String key, String contentType, long contentLength, Duration ttl) {

        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build())
                .build());
    }

    private static HttpResponse<String> put(
            PresignedPutObjectRequest presigned, byte[] body, String contentType) throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder(presigned.url().toURI())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
