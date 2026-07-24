package com.wastecoder.imagepipe.api.fixture;

import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.domain.model.ImageVariant;
import com.wastecoder.imagepipe.shared.ImageStatus;
import com.wastecoder.imagepipe.shared.StorageKeys;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Scenario factories for {@link Image}. No builders: a scenario has a name, not a fluent chain. */
public final class ImageMother {

    public static final Instant CREATED_AT = Instant.parse("2026-07-24T18:00:00Z");
    public static final String CONTENT_TYPE = "image/png";
    public static final long SIZE_BYTES = 204_813L;
    public static final String FILENAME = "cat.png";

    private ImageMother() {}

    /** An upload that was just requested: the presigned URL is out, the bytes are not in. */
    public static Image pending() {
        return pendingWithId(UUID.randomUUID());
    }

    public static Image pendingWithId(UUID id) {
        return Image.pending(id, CONTENT_TYPE, SIZE_BYTES, FILENAME, CREATED_AT);
    }

    /** An upload the worker has finished: READY, with one variant per size. */
    public static Image ready() {
        return readyWithId(UUID.randomUUID());
    }

    public static Image readyWithId(UUID id) {
        List<ImageVariant> variants = Arrays.stream(VariantSize.values())
                .map(size -> new ImageVariant(size, StorageKeys.derivedKey(id, size),
                        size.maxWidth(), size.maxWidth() / 2, 4_096L))
                .toList();

        return Image.restore(id, ImageStatus.READY, CONTENT_TYPE, SIZE_BYTES, FILENAME,
                CREATED_AT, CREATED_AT.plusSeconds(30), variants);
    }
}
