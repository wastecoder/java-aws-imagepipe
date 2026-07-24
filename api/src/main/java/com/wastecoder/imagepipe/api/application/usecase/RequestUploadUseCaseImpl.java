package com.wastecoder.imagepipe.api.application.usecase;

import com.wastecoder.imagepipe.api.application.port.in.RequestUploadUseCase;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUpload;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadCommand;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.domain.model.UploadConstraints;
import java.time.Clock;
import java.util.UUID;

public class RequestUploadUseCaseImpl implements RequestUploadUseCase {

    private final ImageRepository imageRepository;
    private final PresignedUrlGenerator presignedUrlGenerator;
    private final UploadConstraints uploadConstraints;
    private final Clock clock;

    public RequestUploadUseCaseImpl(ImageRepository imageRepository,
                                    PresignedUrlGenerator presignedUrlGenerator,
                                    UploadConstraints uploadConstraints,
                                    Clock clock) {

        this.imageRepository = imageRepository;
        this.presignedUrlGenerator = presignedUrlGenerator;
        this.uploadConstraints = uploadConstraints;
        this.clock = clock;
    }

    @Override
    public RequestUploadView requestUpload(RequestUploadCommand command) {
        // Validate first: neither a row nor a credential should exist for an upload we would refuse.
        String contentType = uploadConstraints.validate(command.contentType(), command.sizeBytes());

        UUID uploadId = UUID.randomUUID();
        Image image = Image.pending(uploadId, contentType, command.sizeBytes(), command.filename(), clock.instant());

        // Persist before signing. If the write fails, no upload credential was ever handed out.
        imageRepository.save(image);

        PresignedUpload upload = presignedUrlGenerator.generate(uploadId, contentType, command.sizeBytes());

        return new RequestUploadView(uploadId, image.status(), upload.url(),
                upload.requiredHeaders(), upload.expiresAt());
    }
}
