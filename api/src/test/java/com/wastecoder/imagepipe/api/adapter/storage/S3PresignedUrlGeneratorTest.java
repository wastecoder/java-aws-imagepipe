package com.wastecoder.imagepipe.api.adapter.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.wastecoder.imagepipe.api.application.port.out.PresignedUpload;
import com.wastecoder.imagepipe.shared.StorageKeys;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Covers the one piece of logic this adapter owns: turning what the SDK signed into what a client
 * must echo. Whether the SDK signs the right things is a question only a real S3 can answer, and
 * S3PresignedUrlGeneratorIntegrationTest asks it.
 */
@ExtendWith(MockitoExtension.class)
class S3PresignedUrlGeneratorTest {

    private static final UUID UPLOAD_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-24T18:05:00Z");
    private static final URI URL = URI.create("http://localhost:4566/imagepipe-raw/uploads/x?X-Amz-Signature=abc");

    @Mock
    private S3Presigner presigner;

    @Mock
    private PresignedPutObjectRequest presigned;

    @Test
    @DisplayName("Given signed headers, when generating, then host is dropped and the others get conventional casing")
    void dropsHostAndRestoresConventionalCasing() {
        givenSignedHeaders(Map.of(
                "host", List.of("imagepipe-raw.s3.amazonaws.com"),
                "content-type", List.of("image/png"),
                "content-length", List.of("204813")));

        PresignedUpload upload = generator().generate(UPLOAD_ID, "image/png", 204_813L);

        assertThat(upload.requiredHeaders())
                .containsOnly(entry("Content-Type", "image/png"), entry("Content-Length", "204813"));
        assertThat(upload.url()).isEqualTo(URL);
        assertThat(upload.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("Given a signed header with no value, when generating, then it is left out")
    void dropsAValuelessHeader() {
        givenSignedHeaders(Map.of(
                "host", List.of("imagepipe-raw.s3.amazonaws.com"),
                "content-type", List.of("image/png"),
                "x-amz-meta-empty", List.of()));

        assertThat(generator().generate(UPLOAD_ID, "image/png", 1L).requiredHeaders())
                .containsOnly(entry("Content-Type", "image/png"));
    }

    @Test
    @DisplayName("Given a signed header outside the known set, when generating, then its name passes through unchanged")
    void passesAnUnknownHeaderThrough() {
        givenSignedHeaders(Map.of(
                "host", List.of("imagepipe-raw.s3.amazonaws.com"),
                "x-amz-meta-upload-id", List.of("abc")));

        assertThat(generator().generate(UPLOAD_ID, "image/png", 1L).requiredHeaders())
                .containsOnly(entry("x-amz-meta-upload-id", "abc"));
    }

    @Test
    @DisplayName("Given only the host was signed, when generating, then no header is required of the client")
    void requiresNoHeaderWhenOnlyHostWasSigned() {
        givenSignedHeaders(Map.of("host", List.of("imagepipe-raw.s3.amazonaws.com")));

        assertThat(generator().generate(UPLOAD_ID, "image/png", 1L).requiredHeaders()).isEmpty();
    }

    @Test
    @DisplayName("Given an upload, when generating, then the signed request carries the configured duration and key")
    void signsTheConfiguredDurationBucketAndKey() {
        givenSignedHeaders(Map.of("host", List.of("imagepipe-raw.s3.amazonaws.com")));

        generator().generate(UPLOAD_ID, "image/png", 204_813L);

        ArgumentCaptor<PutObjectPresignRequest> presignRequest =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(presignRequest.capture());

        PutObjectPresignRequest captured = presignRequest.getValue();
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(captured.putObjectRequest().bucket()).isEqualTo("imagepipe-raw");
        assertThat(captured.putObjectRequest().key()).isEqualTo(StorageKeys.rawKey(UPLOAD_ID));
        assertThat(captured.putObjectRequest().contentType()).isEqualTo("image/png");
        assertThat(captured.putObjectRequest().contentLength()).isEqualTo(204_813L);
    }

    private void givenSignedHeaders(Map<String, List<String>> signedHeaders) {
        given(presigned.signedHeaders()).willReturn(signedHeaders);
        given(presigned.url()).willReturn(toUrl());
        given(presigned.expiration()).willReturn(EXPIRES_AT);
        given(presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);
    }

    private static java.net.URL toUrl() {
        try {
            return URL.toURL();
        } catch (Exception cause) {
            throw new IllegalStateException(cause);
        }
    }

    private S3PresignedUrlGenerator generator() {
        return new S3PresignedUrlGenerator(presigner, "imagepipe-raw", Duration.ofMinutes(5));
    }
}
