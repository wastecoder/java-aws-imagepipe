package com.wastecoder.imagepipe.api.domain.exception;

import java.util.List;
import java.util.Set;

/**
 * The declared content type is not in the allowlist.
 *
 * <p>Carries the allowlist so the Problem Details response can tell the caller what would have been
 * accepted, without the web adapter having to reach back into configuration.
 */
public class UnsupportedContentTypeException extends RuntimeException {

    private final String contentType;
    private final Set<String> allowedContentTypes;

    public UnsupportedContentTypeException(String contentType, Set<String> allowedContentTypes) {
        super("Content type not supported: " + contentType);
        this.contentType = contentType;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    public String contentType() {
        return contentType;
    }

    /** Sorted so the response body is stable, which keeps assertions on it honest. */
    public List<String> allowedContentTypes() {
        return allowedContentTypes.stream().sorted().toList();
    }
}
