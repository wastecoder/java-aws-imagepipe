package com.wastecoder.imagepipe.api.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.wastecoder.imagepipe.api.TestcontainersConfiguration;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.domain.model.ImageVariant;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs against a real PostgreSQL with the Flyway migration applied, so it also proves V1 matches the
 * entity mapping. An in-memory database would prove neither.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ImageRepositoryIntegrationTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * {@code roundTripsAReadyImageWithVariants} above saves and reads back through the same
     * {@link ImageRepository}, which goes through the same Hibernate mapping on both ends. A
     * symmetric mistake — say, swapping the {@code @Column(name = "width")} and
     * {@code @Column(name = "height")} annotations in {@code ImageVariantEntity} — writes through
     * the wrong mapping and reads back through the same wrong mapping, so every value survives the
     * round trip anyway. No JPA round-trip test can see that kind of bug.
     *
     * <p>{@code image_variants} is also a contract with the Phase 2 worker, which will write to it
     * with plain JDBC and raw SQL, bypassing Hibernate entirely. This test reads the row the same
     * way: by column name, with no entity mapping involved, so it fails if a column ever holds the
     * wrong value under its own name — which is exactly the failure mode the worker would hit.
     *
     * <p>{@link ImageMother#readyWithId} builds each variant with {@code width = size.maxWidth()}
     * and {@code height = size.maxWidth() / 2}, which are never equal, so a width/height swap is
     * guaranteed to be detectable here.
     */
    @Test
    @DisplayName("Given a ready image, when reading image_variants with raw sql, then each column holds its own value")
    void writesEachVariantColumnUnderItsOwnName() {
        Image saved = imageRepository.save(ImageMother.ready());

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT width, height, s3_key, bytes
                  FROM image_variants
                 WHERE image_id = ? AND size = ?
                """, saved.id(), VariantSize.SMALL.name());

        ImageVariant expected = saved.variants().stream()
                .filter(variant -> variant.size() == VariantSize.SMALL)
                .findFirst()
                .orElseThrow();

        assertThat(row.get("width")).isEqualTo(expected.width());
        assertThat(row.get("height")).isEqualTo(expected.height());
        assertThat(row.get("s3_key")).isEqualTo(expected.storageKey());
        assertThat(row.get("bytes")).isEqualTo(expected.bytes());
    }
}
