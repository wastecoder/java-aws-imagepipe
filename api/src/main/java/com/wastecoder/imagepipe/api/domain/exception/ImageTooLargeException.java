package com.wastecoder.imagepipe.api.domain.exception;

/** The declared size exceeds what this deployment accepts. */
public class ImageTooLargeException extends RuntimeException {

    private final long sizeBytes;
    private final long maxSizeBytes;

    public ImageTooLargeException(long sizeBytes, long maxSizeBytes) {
        super("Upload of " + sizeBytes + " bytes exceeds the maximum of " + maxSizeBytes);
        this.sizeBytes = sizeBytes;
        this.maxSizeBytes = maxSizeBytes;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public long maxSizeBytes() {
        return maxSizeBytes;
    }
}
