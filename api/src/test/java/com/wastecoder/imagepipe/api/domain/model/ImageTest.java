package com.wastecoder.imagepipe.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.wastecoder.imagepipe.shared.ImageStatus;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-07-24T18:00:00Z");

    @Test
    @DisplayName("Given an upload request, when creating a pending image, then it starts PENDING with no variants")
    void startsPendingWithoutVariants() {
        Image image = Image.pending(ID, "image/png", 1_024L, "cat.png", NOW);

        assertThat(image.id()).isEqualTo(ID);
        assertThat(image.status()).isEqualTo(ImageStatus.PENDING);
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.sizeBytes()).isEqualTo(1_024L);
        assertThat(image.filename()).isEqualTo("cat.png");
        assertThat(image.createdAt()).isEqualTo(NOW);
        assertThat(image.updatedAt()).isEqualTo(NOW);
        assertThat(image.variants()).isEmpty();
    }

    @Test
    @DisplayName("Given no filename, when creating a pending image, then it is accepted as null")
    void acceptsAnAbsentFilename() {
        assertThat(Image.pending(ID, "image/png", 1_024L, null, NOW).filename()).isNull();
    }

    @Test
    @DisplayName("Given stored data, when restoring an image, then every field and variant comes back")
    void restoresEveryField() {
        ImageVariant variant = new ImageVariant(VariantSize.SMALL, "derived/x/small.webp", 320, 180, 4_096L);

        Image image = Image.restore(ID, ImageStatus.READY, "image/png", 1_024L, "cat.png",
                NOW, NOW.plusSeconds(60), List.of(variant));

        assertThat(image.status()).isEqualTo(ImageStatus.READY);
        assertThat(image.updatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(image.variants()).containsExactly(variant);
    }

    @Test
    @DisplayName("Given a restored image, when mutating the list it was built from, then the aggregate is unaffected")
    void copiesTheVariantList() {
        List<ImageVariant> mutable = new ArrayList<>();
        mutable.add(new ImageVariant(VariantSize.SMALL, "derived/x/small.webp", 320, 180, 4_096L));
        Image image = Image.restore(ID, ImageStatus.READY, "image/png", 1_024L, null, NOW, NOW, mutable);

        mutable.clear();

        assertThat(image.variants()).hasSize(1);
    }

    @Test
    @DisplayName("Given a null id, when creating an image, then it is rejected")
    void rejectsANullId() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.pending(null, "image/png", 1_024L, null, NOW))
                .withMessageContaining("id");
    }

    @Test
    @DisplayName("Given a null status, when restoring an image, then it is rejected")
    void rejectsANullStatus() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.restore(ID, null, "image/png", 1_024L, null, NOW, NOW, List.of()))
                .withMessageContaining("status");
    }

    @Test
    @DisplayName("Given a null content type, when creating an image, then it is rejected")
    void rejectsANullContentType() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.pending(ID, null, 1_024L, null, NOW))
                .withMessageContaining("contentType");
    }

    @Test
    @DisplayName("Given a non positive size, when creating an image, then it is rejected")
    void rejectsANonPositiveSize() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Image.pending(ID, "image/png", 0L, null, NOW))
                .withMessageContaining("sizeBytes");
    }

    @Test
    @DisplayName("Given a null creation instant, when creating an image, then it is rejected")
    void rejectsANullCreatedAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.pending(ID, "image/png", 1_024L, null, null))
                .withMessageContaining("createdAt");
    }

    @Test
    @DisplayName("Given a null update instant, when restoring an image, then it is rejected")
    void rejectsANullUpdatedAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.restore(ID, ImageStatus.PENDING, "image/png", 1_024L, null, NOW, null, List.of()))
                .withMessageContaining("updatedAt");
    }

    @Test
    @DisplayName("Given a null variant list, when restoring an image, then it is rejected")
    void rejectsANullVariantList() {
        assertThatNullPointerException()
                .isThrownBy(() -> Image.restore(ID, ImageStatus.PENDING, "image/png", 1_024L, null, NOW, NOW, null))
                .withMessageContaining("variants");
    }
}
