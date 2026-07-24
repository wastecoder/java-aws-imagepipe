package com.wastecoder.imagepipe.api.adapter.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.wastecoder.imagepipe.api.TestcontainersConfiguration;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUpload;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import com.wastecoder.imagepipe.shared.StorageKeys;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Exercises the production adapter against a LocalStack that verifies signatures.
 *
 * <p>This is where the phase's stated criterion is proved: the URL accepts the correct PUT and
 * refuses one whose Content-Type diverges.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class S3PresignedUrlGeneratorIntegrationTest {

    private static final byte[] BODY = "pretend this is a png".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private PresignedUrlGenerator presignedUrlGenerator;

    @Autowired
    private S3Client s3;

    @Test
    @DisplayName("Given an upload id, when generating the url, then content type and length are both required")
    void requiresBothSignedHeaders() {
        PresignedUpload upload = presignedUrlGenerator.generate(UUID.randomUUID(), "image/png", BODY.length);

        assertThat(upload.requiredHeaders()).containsOnlyKeys("Content-Type", "Content-Length");
        assertThat(upload.requiredHeaders())
                .containsEntry("Content-Type", "image/png")
                .containsEntry("Content-Length", String.valueOf(BODY.length));
        assertThat(upload.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Given the required headers, when putting the bytes, then the object lands in the raw bucket")
    void acceptsTheCorrectUpload() throws Exception {
        UUID uploadId = UUID.randomUUID();
        PresignedUpload upload = presignedUrlGenerator.generate(uploadId, "image/png", BODY.length);

        HttpResponse<String> response = put(upload, BODY, upload.requiredHeaders());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(s3.headObject(request -> request
                        .bucket(TestcontainersConfiguration.RAW_BUCKET)
                        .key(StorageKeys.rawKey(uploadId)))
                .contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Given a divergent content type, when putting the bytes, then S3 refuses the signature")
    void refusesADivergentContentType() throws Exception {
        PresignedUpload upload = presignedUrlGenerator.generate(UUID.randomUUID(), "image/png", BODY.length);

        HttpResponse<String> response = put(upload, BODY, Map.of("Content-Type", "image/jpeg"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given fewer bytes than were signed for, when putting them, then S3 refuses the signature")
    void refusesADivergentContentLength() throws Exception {
        PresignedUpload upload = presignedUrlGenerator.generate(UUID.randomUUID(), "image/png", BODY.length);

        HttpResponse<String> response =
                put(upload, "short".getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "image/png"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("SignatureDoesNotMatch");
    }

    /**
     * Content-Length is never set by hand: java.net.http rejects it as a restricted header name and
     * derives it from the body, which is exactly what a real client does.
     */
    private static HttpResponse<String> put(PresignedUpload upload, byte[] body, Map<String, String> headers)
            throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder(upload.url())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach((name, value) -> {
            if (!"Content-Length".equalsIgnoreCase(name)) {
                request.header(name, value);
            }
        });

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
