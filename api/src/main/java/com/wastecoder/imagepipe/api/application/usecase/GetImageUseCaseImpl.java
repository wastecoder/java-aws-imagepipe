package com.wastecoder.imagepipe.api.application.usecase;

import com.wastecoder.imagepipe.api.application.port.in.GetImageUseCase;
import com.wastecoder.imagepipe.api.application.port.out.CdnUrlResolver;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.application.viewmodel.ImageView;
import com.wastecoder.imagepipe.api.application.viewmodel.VariantView;
import com.wastecoder.imagepipe.api.domain.exception.ImageNotFoundException;
import com.wastecoder.imagepipe.api.domain.model.Image;
import java.util.List;
import java.util.UUID;

public class GetImageUseCaseImpl implements GetImageUseCase {

    private final ImageRepository imageRepository;
    private final CdnUrlResolver cdnUrlResolver;

    public GetImageUseCaseImpl(ImageRepository imageRepository, CdnUrlResolver cdnUrlResolver) {
        this.imageRepository = imageRepository;
        this.cdnUrlResolver = cdnUrlResolver;
    }

    @Override
    public ImageView getImage(UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));

        // A pending image simply has no variants yet, so this list comes out empty on its own.
        List<VariantView> variants = image.variants().stream()
                .map(variant -> new VariantView(variant.size(),
                        cdnUrlResolver.resolve(image.id(), variant.size()),
                        variant.width(), variant.height()))
                .toList();

        return new ImageView(image.id(), image.status(), image.contentType(), image.sizeBytes(),
                image.filename(), image.createdAt(), variants);
    }
}
