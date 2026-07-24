package com.wastecoder.imagepipe.api.domain.model;

import com.wastecoder.imagepipe.shared.VariantSize;
import java.util.Objects;

/**
 * One generated variant of an image.
 *
 * <p>A value object: it has no identity beyond the image it belongs to and its size. That is also
 * why the table's primary key is {@code (image_id, size)} — the pair is the identity, and it is what
 * lets the worker upsert idempotently.
 */
public record ImageVariant(VariantSize size, String storageKey, int width, int height, long bytes) {

    public ImageVariant {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive: " + height);
        }
        if (bytes <= 0) {
            throw new IllegalArgumentException("bytes must be positive: " + bytes);
        }
    }
}
