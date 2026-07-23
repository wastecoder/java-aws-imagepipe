package com.wastecoder.imagepipe.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VariantSizeTest {

    @Test
    @DisplayName("Given the variant catalogue, when read, then every size exposes its max width")
    void everySizeExposesItsMaxWidth() {
        assertThat(VariantSize.SMALL.maxWidth()).isEqualTo(320);
        assertThat(VariantSize.MEDIUM.maxWidth()).isEqualTo(640);
        assertThat(VariantSize.LARGE.maxWidth()).isEqualTo(1280);
    }

    @Test
    @DisplayName("Given a variant size, when slugged, then it is the lower-case constant name")
    void slugIsTheLowerCaseConstantName() {
        assertThat(VariantSize.SMALL.slug()).isEqualTo("small");
        assertThat(VariantSize.MEDIUM.slug()).isEqualTo("medium");
        assertThat(VariantSize.LARGE.slug()).isEqualTo("large");
    }
}
