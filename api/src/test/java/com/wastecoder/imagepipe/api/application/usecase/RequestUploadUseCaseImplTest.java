package com.wastecoder.imagepipe.api.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUpload;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadCommand;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;
import com.wastecoder.imagepipe.api.domain.exception.ImageTooLargeException;
import com.wastecoder.imagepipe.api.domain.exception.UnsupportedContentTypeException;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.domain.model.UploadConstraints;
import com.wastecoder.imagepipe.shared.ImageStatus;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestUploadUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-07-24T18:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final UploadConstraints CONSTRAINTS =
            new UploadConstraints(Set.of("image/png", "image/jpeg"), 10_485_760L);

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private PresignedUrlGenerator presignedUrlGenerator;

    @Captor
    private ArgumentCaptor<Image> savedImage;

    private RequestUploadUseCaseImpl useCase() {
        return new RequestUploadUseCaseImpl(imageRepository, presignedUrlGenerator, CONSTRAINTS, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Given a valid command, when requesting an upload, then a pending image is stored")
    void storesAPendingImage() {
        givenAPresignedUpload();

        useCase().requestUpload(new RequestUploadCommand("image/png", 204_813L, "cat.png"));

        verify(imageRepository).save(savedImage.capture());
        Image stored = savedImage.getValue();
        assertThat(stored.status()).isEqualTo(ImageStatus.PENDING);
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.sizeBytes()).isEqualTo(204_813L);
        assertThat(stored.filename()).isEqualTo("cat.png");
        assertThat(stored.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Given a valid command, when requesting an upload, then the view carries the signed instructions")
    void returnsTheSignedInstructions() {
        givenAPresignedUpload();

        RequestUploadView view = useCase().requestUpload(new RequestUploadCommand("image/png", 204_813L, "cat.png"));

        verify(imageRepository).save(savedImage.capture());
        assertThat(view.uploadId()).isEqualTo(savedImage.getValue().id());
        assertThat(view.status()).isEqualTo(ImageStatus.PENDING);
        assertThat(view.uploadUrl()).isEqualTo(URI.create("http://localstack/imagepipe-raw/uploads/x"));
        assertThat(view.requiredHeaders())
                .containsExactlyInAnyOrderEntriesOf(Map.of("Content-Type", "image/png", "Content-Length", "204813"));
        assertThat(view.expiresAt()).isEqualTo(NOW.plusSeconds(300));
    }

    @Test
    @DisplayName("Given a content type in another case, when requesting an upload, then the normalised one is signed")
    void signsTheNormalisedContentType() {
        givenAPresignedUpload();

        useCase().requestUpload(new RequestUploadCommand("IMAGE/PNG", 204_813L, null));

        verify(presignedUrlGenerator).generate(any(), org.mockito.ArgumentMatchers.eq("image/png"), anyLong());
    }

    @Test
    @DisplayName("Given a type outside the allowlist, when requesting an upload, then nothing is stored or signed")
    void refusesAnUnsupportedTypeBeforeAnySideEffect() {
        assertThatThrownBy(() -> useCase().requestUpload(new RequestUploadCommand("application/zip", 10L, null)))
                .isInstanceOf(UnsupportedContentTypeException.class);

        verify(imageRepository, never()).save(any());
        verify(presignedUrlGenerator, never()).generate(any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Given a size above the ceiling, when requesting an upload, then nothing is stored or signed")
    void refusesAnOversizedUploadBeforeAnySideEffect() {
        assertThatThrownBy(() -> useCase().requestUpload(new RequestUploadCommand("image/png", 10_485_761L, null)))
                .isInstanceOf(ImageTooLargeException.class);

        verify(imageRepository, never()).save(any());
        verify(presignedUrlGenerator, never()).generate(any(), anyString(), anyLong());
    }

    private void givenAPresignedUpload() {
        given(presignedUrlGenerator.generate(any(), anyString(), anyLong()))
                .willReturn(new PresignedUpload(
                        URI.create("http://localstack/imagepipe-raw/uploads/x"),
                        Map.of("Content-Type", "image/png", "Content-Length", "204813"),
                        NOW.plusSeconds(300)));
    }
}
