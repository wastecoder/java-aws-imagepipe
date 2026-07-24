package com.wastecoder.imagepipe.api.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.wastecoder.imagepipe.api.TestcontainersConfiguration;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Runs against a real PostgreSQL with the Flyway migration applied, so it also proves V1 matches the
 * entity mapping. An in-memory database would prove neither.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ImageRepositoryIntegrationTest {

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("Given a pending image, when saving and reading it back, then every field survives the round trip")
    void roundTripsAPendingImage() {
        Image saved = imageRepository.save(ImageMother.pending());

        Optional<Image> found = imageRepository.findById(saved.id());

        assertThat(found).get().usingRecursiveComparison().isEqualTo(saved);
    }

    @Test
    @DisplayName("Given a ready image, when saving and reading it back, then its three variants survive")
    void roundTripsAReadyImageWithVariants() {
        Image saved = imageRepository.save(ImageMother.ready());

        Optional<Image> found = imageRepository.findById(saved.id());

        assertThat(found).get().usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(saved);
    }

    @Test
    @DisplayName("Given an unknown id, when reading an image, then nothing comes back")
    void returnsEmptyForAnUnknownId() {
        assertThat(imageRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
