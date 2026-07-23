package com.wastecoder.imagepipe.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageStatusTest {

    @Test
    @DisplayName("Given the image lifecycle, when listed, then it holds exactly pending, ready and failed")
    void lifecycleHoldsExactlyThreeStates() {
        assertThat(ImageStatus.values())
            .containsExactly(ImageStatus.PENDING, ImageStatus.READY, ImageStatus.FAILED);
    }
}
