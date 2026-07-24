package com.wastecoder.imagepipe.api.adapter.persistence.mapper;

import com.wastecoder.imagepipe.api.adapter.persistence.entity.ImageEntity;
import com.wastecoder.imagepipe.api.adapter.persistence.entity.ImageVariantEntity;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.domain.model.ImageVariant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Translates between the aggregate and its rows. The only place that knows both shapes. */
@Component
public class ImageEntityMapper {

    public ImageEntity toEntity(Image image) {
        ImageEntity entity = new ImageEntity();
        entity.setId(image.id());
        entity.setStatus(image.status());
        entity.setContentType(image.contentType());
        entity.setSizeBytes(image.sizeBytes());
        entity.setFilename(image.filename());
        entity.setCreatedAt(image.createdAt());
        entity.setUpdatedAt(image.updatedAt());
        // Mutable on purpose: Hibernate replaces and mutates this collection when it manages the
        // entity, and an immutable List.of would blow up at flush time.
        entity.setVariants(new ArrayList<>(
                image.variants().stream().map(ImageEntityMapper::toEntity).toList()));
        return entity;
    }

    public Image toDomain(ImageEntity entity) {
        List<ImageVariant> variants = entity.getVariants().stream()
                .map(ImageEntityMapper::toDomain)
                .toList();

        return Image.restore(entity.getId(), entity.getStatus(), entity.getContentType(),
                entity.getSizeBytes(), entity.getFilename(), entity.getCreatedAt(),
                entity.getUpdatedAt(), variants);
    }

    private static ImageVariantEntity toEntity(ImageVariant variant) {
        ImageVariantEntity entity = new ImageVariantEntity();
        entity.setSize(variant.size());
        entity.setS3Key(variant.storageKey());
        entity.setWidth(variant.width());
        entity.setHeight(variant.height());
        entity.setBytes(variant.bytes());
        return entity;
    }

    private static ImageVariant toDomain(ImageVariantEntity entity) {
        return new ImageVariant(entity.getSize(), entity.getS3Key(), entity.getWidth(),
                entity.getHeight(), entity.getBytes());
    }
}
