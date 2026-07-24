package com.wastecoder.imagepipe.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wastecoder.imagepipe.api.domain.exception.ImageTooLargeException;
import com.wastecoder.imagepipe.api.domain.exception.UnsupportedContentTypeException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UploadConstraintsTest {

    private static final UploadConstraints CONSTRAINTS =
            new UploadConstraints(Set.of("image/jpeg", "image/png", "image/webp"), 10_485_760L);

    @Test
    @DisplayName("Given an allowed type within the ceiling, when validating, then the normalised type is returned")
    void acceptsAnAllowedType() {
        assertThat(CONSTRAINTS.validate("image/png", 1_024L)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Given a type with different case and spaces, when validating, then it is normalised")
    void normalisesTheDeclaredType() {
        assertThat(CONSTRAINTS.validate("  IMAGE/PNG  ", 1_024L)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Given an allowlist written in mixed case, when validating, then it still matches")
    void normalisesTheAllowlistToo() {
        UploadConstraints constraints = new UploadConstraints(Set.of("Image/PNG"), 10L);

        assertThat(constraints.validate("image/png", 5L)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Given a size exactly at the ceiling, when validating, then it is accepted")
    void acceptsTheExactCeiling() {
        assertThat(CONSTRAINTS.validate("image/png", 10_485_760L)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Given a type outside the allowlist, when validating, then it is refused")
    void refusesATypeOutsideTheAllowlist() {
        assertThatThrownBy(() -> CONSTRAINTS.validate("application/zip", 1_024L))
                .isInstanceOf(UnsupportedContentTypeException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(UnsupportedContentTypeException.class))
                .satisfies(exception -> {
                    assertThat(exception.contentType()).isEqualTo("application/zip");
                    assertThat(exception.allowedContentTypes())
                            .containsExactly("image/jpeg", "image/png", "image/webp");
                });
    }

    @Test
    @DisplayName("Given a size above the ceiling, when validating, then it is refused")
    void refusesASizeAboveTheCeiling() {
        assertThatThrownBy(() -> CONSTRAINTS.validate("image/png", 10_485_761L))
                .isInstanceOf(ImageTooLargeException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ImageTooLargeException.class))
                .satisfies(exception -> {
                    assertThat(exception.sizeBytes()).isEqualTo(10_485_761L);
                    assertThat(exception.maxSizeBytes()).isEqualTo(10_485_760L);
                });
    }

    @Test
    @DisplayName("Given a non positive size, when validating, then it is rejected as an argument error")
    void rejectsANonPositiveSize() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CONSTRAINTS.validate("image/png", 0L))
                .withMessageContaining("sizeBytes");
    }

    @Test
    @DisplayName("Given a null content type, when validating, then it is rejected")
    void rejectsANullContentType() {
        assertThatNullPointerException()
                .isThrownBy(() -> CONSTRAINTS.validate(null, 1_024L))
                .withMessageContaining("contentType");
    }

    @Test
    @DisplayName("Given a null allowlist, when creating the constraints, then it is rejected")
    void rejectsANullAllowlist() {
        assertThatNullPointerException()
                .isThrownBy(() -> new UploadConstraints(null, 10L))
                .withMessageContaining("allowedContentTypes");
    }

    @Test
    @DisplayName("Given an empty allowlist, when creating the constraints, then it is rejected")
    void rejectsAnEmptyAllowlist() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UploadConstraints(Set.of(), 10L))
                .withMessageContaining("allowedContentTypes");
    }

    @Test
    @DisplayName("Given a non positive ceiling, when creating the constraints, then it is rejected")
    void rejectsANonPositiveCeiling() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UploadConstraints(Set.of("image/png"), 0L))
                .withMessageContaining("maxSizeBytes");
    }
}
