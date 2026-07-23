package com.wastecoder.imagepipe.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StorageKeysTest {

    private static final UUID UPLOAD_ID = UUID.fromString("3f2504e0-4f89-41d3-9a0c-0305e82c3301");

    @Test
    @DisplayName("Given an upload id, when the raw key is derived, then it is prefixed with uploads/")
    void rawKeyIsPrefixedWithUploads() {
        assertThat(StorageKeys.rawKey(UPLOAD_ID)).isEqualTo("uploads/" + UPLOAD_ID);
    }

    @Test
    @DisplayName("Given an upload id and a size, when the derived key is built, then it follows derived/{id}/{slug}.webp")
    void derivedKeyFollowsTheDocumentedLayout() {
        assertThat(StorageKeys.derivedKey(UPLOAD_ID, VariantSize.SMALL))
            .isEqualTo("derived/" + UPLOAD_ID + "/small.webp");
        assertThat(StorageKeys.derivedKey(UPLOAD_ID, VariantSize.MEDIUM))
            .isEqualTo("derived/" + UPLOAD_ID + "/medium.webp");
        assertThat(StorageKeys.derivedKey(UPLOAD_ID, VariantSize.LARGE))
            .isEqualTo("derived/" + UPLOAD_ID + "/large.webp");
    }

    @Test
    @DisplayName("Given the same input twice, when keys are derived, then both are identical")
    void derivationIsDeterministic() {
        assertThat(StorageKeys.derivedKey(UPLOAD_ID, VariantSize.SMALL))
            .isEqualTo(StorageKeys.derivedKey(UPLOAD_ID, VariantSize.SMALL));
        assertThat(StorageKeys.rawKey(UPLOAD_ID)).isEqualTo(StorageKeys.rawKey(UPLOAD_ID));
    }

    @Test
    @DisplayName("Given a raw key built from an id, when parsed back, then the original id is returned")
    void rawKeyRoundTrips() {
        assertThat(StorageKeys.uploadIdFromRawKey(StorageKeys.rawKey(UPLOAD_ID))).isEqualTo(UPLOAD_ID);
    }

    @Test
    @DisplayName("Given a key without the uploads prefix, when parsed, then it is rejected")
    void parsingRejectsAKeyWithoutThePrefix() {
        assertThatThrownBy(() -> StorageKeys.uploadIdFromRawKey(UPLOAD_ID.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uploads/");
    }

    @Test
    @DisplayName("Given a key whose suffix is not a uuid, when parsed, then it is rejected")
    void parsingRejectsAMalformedUploadId() {
        assertThatThrownBy(() -> StorageKeys.uploadIdFromRawKey("uploads/not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid upload id");
    }

    @Test
    @DisplayName("Given a blank key, when parsed, then it is rejected")
    void parsingRejectsABlankKey() {
        assertThatThrownBy(() -> StorageKeys.uploadIdFromRawKey(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given a null argument, when any key is derived, then it is rejected")
    void derivationRejectsNulls() {
        assertThatThrownBy(() -> StorageKeys.rawKey(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StorageKeys.uploadIdFromRawKey(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StorageKeys.derivedKey(null, VariantSize.SMALL))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StorageKeys.derivedKey(UPLOAD_ID, null))
            .isInstanceOf(NullPointerException.class);
    }
}
