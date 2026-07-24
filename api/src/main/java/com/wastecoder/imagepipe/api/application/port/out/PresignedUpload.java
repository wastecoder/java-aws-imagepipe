package com.wastecoder.imagepipe.api.application.port.out;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a client needs to upload the bytes itself.
 *
 * <p>{@code requiredHeaders} is not decoration: once anything beyond the host is signed, the URL
 * alone is not enough, and a request missing a signed header is refused with 403. The adapter must
 * derive this map from what was actually signed rather than assemble it by hand.
 */
public record PresignedUpload(URI url, Map<String, String> requiredHeaders, Instant expiresAt) {

    public PresignedUpload {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        requiredHeaders = Map.copyOf(Objects.requireNonNull(requiredHeaders, "requiredHeaders must not be null"));
    }
}
