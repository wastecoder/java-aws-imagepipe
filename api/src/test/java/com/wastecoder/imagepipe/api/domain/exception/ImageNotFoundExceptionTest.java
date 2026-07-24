package com.wastecoder.imagepipe.api.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageNotFoundExceptionTest {

    @Test
    @DisplayName("Given an image id, when creating the exception, then it is kept and reported in the message")
    void keepsTheImageId() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");

        ImageNotFoundException exception = new ImageNotFoundException(id);

        assertThat(exception.imageId()).isEqualTo(id);
        assertThat(exception.getMessage()).contains(id.toString());
    }
}
