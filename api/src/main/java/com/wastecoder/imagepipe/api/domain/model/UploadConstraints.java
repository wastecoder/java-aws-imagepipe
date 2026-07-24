package com.wastecoder.imagepipe.api.domain.model;

import com.wastecoder.imagepipe.api.domain.exception.ImageTooLargeException;
import com.wastecoder.imagepipe.api.domain.exception.UnsupportedContentTypeException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What this deployment accepts for upload.
 *
 * <p>The values are configuration — they change per environment and should not need a recompile.
 * The rule is not: refusing an unsupported type is a domain decision, and it is what raises the
 * domain exceptions. Keeping it here makes it testable without Spring and puts it under the domain
 * coverage gate, which is where a rule this load-bearing belongs.
 */
public record UploadConstraints(Set<String> allowedContentTypes, long maxSizeBytes) {

    public UploadConstraints {
        Objects.requireNonNull(allowedContentTypes, "allowedContentTypes must not be null");
        if (allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("allowedContentTypes must not be empty");
        }
        if (maxSizeBytes <= 0) {
            throw new IllegalArgumentException("maxSizeBytes must be positive: " + maxSizeBytes);
        }
        allowedContentTypes = allowedContentTypes.stream()
                .map(UploadConstraints::normalise)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks the declared type against the allowlist and the declared size against the ceiling.
     *
     * <p>The returned value is what gets signed, so it must never be the raw client string: SigV4
     * compares header values byte for byte, and {@code image/png} is a different signature from
     * {@code IMAGE/PNG }.
     *
     * @return the normalised content type, ready to be signed
     */
    public String validate(String contentType, long sizeBytes) {
        String normalised = normalise(Objects.requireNonNull(contentType, "contentType must not be null"));

        if (!allowedContentTypes.contains(normalised)) {
            throw new UnsupportedContentTypeException(normalised, allowedContentTypes);
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive: " + sizeBytes);
        }
        if (sizeBytes > maxSizeBytes) {
            throw new ImageTooLargeException(sizeBytes, maxSizeBytes);
        }
        return normalised;
    }

    private static String normalise(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }
}
