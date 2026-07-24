package com.wastecoder.imagepipe.api.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PresignedUploadTest {

    @Test
    @DisplayName("Given valid values, when creating a presigned upload, then every component is kept")
    void keepsItsComponents() {
        URI url = URI.create("https://raw.example.com/upload");
        Map<String, String> headers = Map.of("Content-Type", "image/png");
        Instant expiresAt = Instant.parse("2026-07-24T10:00:00Z");

        PresignedUpload upload = new PresignedUpload(url, headers, expiresAt);

        assertThat(upload.url()).isEqualTo(url);
        assertThat(upload.requiredHeaders()).isEqualTo(headers);
        assertThat(upload.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("Given a null url, when creating a presigned upload, then it is rejected")
    void rejectsANullUrl() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PresignedUpload(null, Map.of(), Instant.now()))
                .withMessageContaining("url");
    }

    @Test
    @DisplayName("Given a null required headers map, when creating a presigned upload, then it is rejected")
    void rejectsANullRequiredHeaders() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PresignedUpload(URI.create("https://raw.example.com/upload"), null, Instant.now()))
                .withMessageContaining("requiredHeaders");
    }

    @Test
    @DisplayName("Given a null expiresAt, when creating a presigned upload, then it is rejected")
    void rejectsANullExpiresAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PresignedUpload(URI.create("https://raw.example.com/upload"), Map.of(), null))
                .withMessageContaining("expiresAt");
    }

    @Test
    @DisplayName(
            "Given a mutable map used to build a presigned upload, when the caller mutates it afterwards, then the "
                    + "record's headers stay unchanged")
    void copiesTheRequiredHeadersDefensively() {
        Map<String, String> mutableHeaders = new HashMap<>();
        mutableHeaders.put("Content-Type", "image/png");

        PresignedUpload upload = new PresignedUpload(
                URI.create("https://raw.example.com/upload"), mutableHeaders, Instant.now());
        mutableHeaders.put("Content-Type", "image/jpeg");
        mutableHeaders.put("x-amz-acl", "private");

        assertThat(upload.requiredHeaders()).containsExactly(Map.entry("Content-Type", "image/png"));
    }
}
