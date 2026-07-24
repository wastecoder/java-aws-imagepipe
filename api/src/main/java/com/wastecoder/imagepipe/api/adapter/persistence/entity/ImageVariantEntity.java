package com.wastecoder.imagepipe.api.adapter.persistence.entity;

import com.wastecoder.imagepipe.shared.VariantSize;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A row of {@code image_variants}, owned by {@link ImageEntity}.
 *
 * <p>Embeddable rather than an entity, because a variant is a value object: its identity is the
 * (image, size) pair and nothing else. That also spares a composite key class that would pay for
 * nothing.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ImageVariantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 16)
    private VariantSize size;

    @Column(name = "s3_key", nullable = false, length = 255)
    private String s3Key;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private long bytes;
}
