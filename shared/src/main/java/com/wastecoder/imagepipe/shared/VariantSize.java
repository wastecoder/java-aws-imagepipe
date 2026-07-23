package com.wastecoder.imagepipe.shared;

import java.util.Locale;

/**
 * The variants generated for every uploaded image.
 *
 * <p>The width lives here, in the kernel, because it is domain data: the worker needs it to
 * resize and the API exposes it. The slug is what goes into the storage key, so renaming a
 * constant renames every object written from then on.
 */
public enum VariantSize {

    SMALL(320),
    MEDIUM(640),
    LARGE(1280);

    private final int maxWidth;

    VariantSize(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    /** Maximum width in pixels. The aspect ratio of the original is preserved. */
    public int maxWidth() {
        return maxWidth;
    }

    /** The lower-case name used as the file name inside the storage key. */
    public String slug() {
        return name().toLowerCase(Locale.ROOT);
    }
}
