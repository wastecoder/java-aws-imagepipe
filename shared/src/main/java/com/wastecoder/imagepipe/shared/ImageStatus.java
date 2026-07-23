package com.wastecoder.imagepipe.shared;

/**
 * Lifecycle of an image, from the upload request to the generated variants.
 *
 * <p>A corrupt image or an unsupported format is a permanent failure: the row is marked
 * {@link #FAILED} and the queue message is acknowledged, never returned.
 */
public enum ImageStatus {

    /** The upload was requested and the presigned URL issued; the bytes may not have arrived yet. */
    PENDING,

    /** Every variant was generated and written to the derived bucket. */
    READY,

    /** Processing failed permanently. Retrying would not help. */
    FAILED,
}
