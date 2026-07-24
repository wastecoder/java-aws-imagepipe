package com.wastecoder.imagepipe.api.adapter.config;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything about this deployment that is not code.
 *
 * <p>{@code aws.endpoint}, {@code accessKey} and {@code secretKey} are null outside the local
 * profile and the tests: on ECS the presigner talks to real S3 with the task role.
 */
@ConfigurationProperties("imagepipe")
public record ImagepipeProperties(Upload upload, Aws aws, Cdn cdn) {

    public record Upload(Set<String> allowedContentTypes, long maxSizeBytes, Duration urlExpiration) {}

    public record Aws(String region, String rawBucket, String endpoint, String accessKey, String secretKey) {}

    public record Cdn(String baseUrl) {}
}
