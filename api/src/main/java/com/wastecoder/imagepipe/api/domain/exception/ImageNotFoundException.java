package com.wastecoder.imagepipe.api.domain.exception;

import java.util.UUID;

/** No image exists for the requested id. HTTP-agnostic: the web adapter decides it means 404. */
public class ImageNotFoundException extends RuntimeException {

    private final UUID imageId;

    public ImageNotFoundException(UUID imageId) {
        super("No image with id " + imageId);
        this.imageId = imageId;
    }

    public UUID imageId() {
        return imageId;
    }
}
