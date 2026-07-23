package com.wastecoder.imagepipe.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Deterministic derivation of the S3 object keys.
 *
 * <p>This is what makes the worker idempotent by construction. SQS delivery is at-least-once and
 * an S3 event can arrive more than once; because the destination key is a pure function of the
 * upload id and the variant, reprocessing overwrites the same object with the same bytes.
 *
 * <p>The API needs this to build CloudFront URLs and the worker needs it to write variants, which
 * is why it lives in the kernel rather than in either of them.
 */
public final class StorageKeys {

    /** Prefix of every object in the raw bucket. The task role is scoped to it. */
    public static final String RAW_PREFIX = "uploads/";

    /** Prefix of every object in the derived bucket. */
    public static final String DERIVED_PREFIX = "derived/";

    /** Every variant is written in this format, whatever the original was. */
    public static final String VARIANT_EXTENSION = ".webp";

    // Empty on purpose. A body here (a thrown AssertionError, say) would stop JaCoCo from
    // filtering the constructor out, and the uncovered line would sink the kernel below its gate.
    private StorageKeys() {}

    /**
     * Key of the original upload. It carries no extension: the {@code Content-Type} travels signed
     * in the presigned URL, so the worker never has to guess one to fetch the object.
     */
    public static String rawKey(UUID uploadId) {
        Objects.requireNonNull(uploadId, "uploadId must not be null");
        return RAW_PREFIX + uploadId;
    }

    /**
     * Extracts the upload id from a raw key. The worker receives the object key in the S3 event
     * and has nothing else to correlate the message with the database row.
     *
     * @throws IllegalArgumentException if the key is not shaped like {@code uploads/{uuid}}
     */
    public static UUID uploadIdFromRawKey(String rawKey) {
        Objects.requireNonNull(rawKey, "rawKey must not be null");
        if (!rawKey.startsWith(RAW_PREFIX)) {
            throw new IllegalArgumentException("Key does not start with " + RAW_PREFIX + ": " + rawKey);
        }
        String candidate = rawKey.substring(RAW_PREFIX.length());
        try {
            return UUID.fromString(candidate);
        } catch (IllegalArgumentException cause) {
            throw new IllegalArgumentException("Key does not contain a valid upload id: " + rawKey, cause);
        }
    }

    /** Key of a generated variant: {@code derived/{uploadId}/{slug}.webp}. */
    public static String derivedKey(UUID uploadId, VariantSize size) {
        Objects.requireNonNull(uploadId, "uploadId must not be null");
        Objects.requireNonNull(size, "size must not be null");
        return DERIVED_PREFIX + uploadId + "/" + size.slug() + VARIANT_EXTENSION;
    }
}
