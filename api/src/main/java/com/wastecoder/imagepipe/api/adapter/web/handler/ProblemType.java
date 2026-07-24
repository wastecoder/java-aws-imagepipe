package com.wastecoder.imagepipe.api.adapter.web.handler;

import java.net.URI;

/**
 * The taxonomy behind the {@code type} member of every Problem Details response.
 *
 * <p>The URI is an identifier, not an address: nothing has to be served from it. What matters is
 * that a client can branch on it without parsing prose.
 */
public enum ProblemType {

    INVALID_PAYLOAD("invalid-payload", "Invalid payload"),
    UNSUPPORTED_CONTENT_TYPE("unsupported-content-type", "Unsupported content type"),
    IMAGE_TOO_LARGE("image-too-large", "Image too large"),
    IMAGE_NOT_FOUND("image-not-found", "Image not found");

    private static final String BASE_URI = "https://imagepipe.wastecoder.com/problems/";

    private final String slug;
    private final String title;

    ProblemType(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public URI uri() {
        return URI.create(BASE_URI + slug);
    }

    public String title() {
        return title;
    }
}
