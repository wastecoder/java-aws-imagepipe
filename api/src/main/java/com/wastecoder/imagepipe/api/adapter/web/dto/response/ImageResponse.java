package com.wastecoder.imagepipe.api.adapter.web.dto.response;

import com.wastecoder.imagepipe.api.application.viewmodel.ImageView;
import com.wastecoder.imagepipe.shared.ImageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        ImageStatus status,
        String contentType,
        long sizeBytes,
        String filename,
        Instant createdAt,

        @Schema(description = "Empty until the worker has generated the variants and moved the "
                + "image to READY.")
        List<ImageVariantResponse> variants) {

    public static ImageResponse from(ImageView view) {
        return new ImageResponse(view.id(), view.status(), view.contentType(), view.sizeBytes(),
                view.filename(), view.createdAt(),
                view.variants().stream().map(ImageVariantResponse::from).toList());
    }
}
