package com.wastecoder.imagepipe.api.domain.model;

import com.wastecoder.imagepipe.shared.ImageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The aggregate root: one upload, its metadata, and the variants generated from it.
 *
 * <p>It lives here and not in {@code shared} because it is the API's aggregate. The worker owns no
 * part of it and reaches the same rows through plain JDBC.
 *
 * <p>Written by hand rather than generated. Generating setters on an aggregate that has invariants
 * would be a hole straight through them.
 */
public final class Image {

    private final UUID id;
    private final ImageStatus status;
    private final String contentType;
    private final long sizeBytes;
    private final String filename;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<ImageVariant> variants;

    private Image(UUID id, ImageStatus status, String contentType, long sizeBytes, String filename,
                  Instant createdAt, Instant updatedAt, List<ImageVariant> variants) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive: " + sizeBytes);
        }
        this.sizeBytes = sizeBytes;
        this.filename = filename;   // optional: the raw key carries no extension either
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.variants = List.copyOf(Objects.requireNonNull(variants, "variants must not be null"));
    }

    /** A freshly requested upload: the presigned URL was issued, the bytes have not arrived yet. */
    public static Image pending(UUID id, String contentType, long sizeBytes, String filename, Instant now) {
        return new Image(id, ImageStatus.PENDING, contentType, sizeBytes, filename, now, now, List.of());
    }

    /** Rebuilds an aggregate read back from storage. Only the persistence adapter needs this. */
    public static Image restore(UUID id, ImageStatus status, String contentType, long sizeBytes,
                                String filename, Instant createdAt, Instant updatedAt,
                                List<ImageVariant> variants) {

        return new Image(id, status, contentType, sizeBytes, filename, createdAt, updatedAt, variants);
    }

    public UUID id() {
        return id;
    }

    public ImageStatus status() {
        return status;
    }

    public String contentType() {
        return contentType;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String filename() {
        return filename;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<ImageVariant> variants() {
        return variants;
    }
}
