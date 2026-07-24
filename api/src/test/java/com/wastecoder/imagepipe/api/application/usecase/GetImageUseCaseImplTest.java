package com.wastecoder.imagepipe.api.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.wastecoder.imagepipe.api.application.port.out.CdnUrlResolver;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.application.viewmodel.ImageView;
import com.wastecoder.imagepipe.api.domain.exception.ImageNotFoundException;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import com.wastecoder.imagepipe.shared.ImageStatus;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetImageUseCaseImplTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private CdnUrlResolver cdnUrlResolver;

    private GetImageUseCaseImpl useCase() {
        return new GetImageUseCaseImpl(imageRepository, cdnUrlResolver);
    }

    @Test
    @DisplayName("Given a pending image, when reading it, then the metadata comes back with no variants")
    void returnsMetadataWithoutVariants() {
        Image image = ImageMother.pendingWithId(ID);
        given(imageRepository.findById(ID)).willReturn(Optional.of(image));

        ImageView view = useCase().getImage(ID);

        assertThat(view.id()).isEqualTo(ID);
        assertThat(view.status()).isEqualTo(ImageStatus.PENDING);
        assertThat(view.contentType()).isEqualTo(ImageMother.CONTENT_TYPE);
        assertThat(view.sizeBytes()).isEqualTo(ImageMother.SIZE_BYTES);
        assertThat(view.filename()).isEqualTo(ImageMother.FILENAME);
        assertThat(view.createdAt()).isEqualTo(ImageMother.CREATED_AT);
        assertThat(view.variants()).isEmpty();
    }

    @Test
    @DisplayName("Given a ready image, when reading it, then every variant carries its cdn url")
    void resolvesTheCdnUrlOfEveryVariant() {
        given(imageRepository.findById(ID)).willReturn(Optional.of(ImageMother.readyWithId(ID)));
        for (VariantSize size : VariantSize.values()) {
            given(cdnUrlResolver.resolve(ID, size)).willReturn(URI.create("https://cdn/" + size.slug()));
        }

        ImageView view = useCase().getImage(ID);

        assertThat(view.status()).isEqualTo(ImageStatus.READY);
        assertThat(view.variants())
                .extracting(variant -> variant.size().slug(), variant -> variant.url().toString())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("small", "https://cdn/small"),
                        org.assertj.core.groups.Tuple.tuple("medium", "https://cdn/medium"),
                        org.assertj.core.groups.Tuple.tuple("large", "https://cdn/large"));
    }

    @Test
    @DisplayName("Given no image for the id, when reading it, then it is reported as not found")
    void reportsAMissingImage() {
        given(imageRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().getImage(ID))
                .isInstanceOf(ImageNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }
}
