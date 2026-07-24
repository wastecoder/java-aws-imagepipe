package com.wastecoder.imagepipe.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.wastecoder.imagepipe.shared.VariantSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageVariantTest {

    @Test
    @DisplayName("Given valid values, when creating a variant, then every component is kept")
    void keepsItsComponents() {
        ImageVariant variant = new ImageVariant(VariantSize.SMALL, "derived/x/small.webp", 320, 180, 4_096L);

        assertThat(variant.size()).isEqualTo(VariantSize.SMALL);
        assertThat(variant.storageKey()).isEqualTo("derived/x/small.webp");
        assertThat(variant.width()).isEqualTo(320);
        assertThat(variant.height()).isEqualTo(180);
        assertThat(variant.bytes()).isEqualTo(4_096L);
    }

    @Test
    @DisplayName("Given a null size, when creating a variant, then it is rejected")
    void rejectsANullSize() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ImageVariant(null, "derived/x/small.webp", 320, 180, 4_096L))
                .withMessageContaining("size");
    }

    @Test
    @DisplayName("Given a null storage key, when creating a variant, then it is rejected")
    void rejectsANullStorageKey() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ImageVariant(VariantSize.SMALL, null, 320, 180, 4_096L))
                .withMessageContaining("storageKey");
    }

    @Test
    @DisplayName("Given a non positive width, when creating a variant, then it is rejected")
    void rejectsANonPositiveWidth() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImageVariant(VariantSize.SMALL, "k", 0, 180, 4_096L))
                .withMessageContaining("width");
    }

    @Test
    @DisplayName("Given a non positive height, when creating a variant, then it is rejected")
    void rejectsANonPositiveHeight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImageVariant(VariantSize.SMALL, "k", 320, 0, 4_096L))
                .withMessageContaining("height");
    }

    @Test
    @DisplayName("Given non positive bytes, when creating a variant, then it is rejected")
    void rejectsNonPositiveBytes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImageVariant(VariantSize.SMALL, "k", 320, 180, 0L))
                .withMessageContaining("bytes");
    }
}
