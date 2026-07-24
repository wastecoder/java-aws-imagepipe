package com.wastecoder.imagepipe.api.adapter.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wastecoder.imagepipe.api.adapter.persistence.entity.ImageEntity;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageEntityMapperTest {

    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final ImageEntityMapper mapper = new ImageEntityMapper();

    @Test
    @DisplayName("Given a pending image, when mapping to entity and back, then it is unchanged")
    void roundTripsAPendingImage() {
        Image original = ImageMother.pendingWithId(ID);

        Image restored = mapper.toDomain(mapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    @DisplayName("Given a ready image with variants, when mapping to entity and back, then every variant survives")
    void roundTripsAReadyImage() {
        Image original = ImageMother.readyWithId(ID);

        Image restored = mapper.toDomain(mapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(original);
    }

    @Test
    @DisplayName("Given an image without a filename, when mapping to entity, then the column stays null")
    void keepsAnAbsentFilenameNull() {
        Image original = Image.pending(ID, "image/png", 1_024L, null, ImageMother.CREATED_AT);

        ImageEntity entity = mapper.toEntity(original);

        assertThat(entity.getFilename()).isNull();
        assertThat(mapper.toDomain(entity).filename()).isNull();
    }
}
