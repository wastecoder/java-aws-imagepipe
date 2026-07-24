package com.wastecoder.imagepipe.api.adapter.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestUploadRequest(

        @NotBlank
        @Schema(description = "Media type of the bytes about to be uploaded. It gets signed, so the "
                + "PUT must carry exactly this value.", example = "image/png")
        String contentType,

        @Positive
        @Schema(description = "Exact size in bytes. It gets signed too: the PUT must carry exactly "
                + "this many bytes, no more and no fewer.", example = "204813")
        long sizeBytes,

        @Size(max = 255)
        @Schema(description = "Original file name. Optional, kept as metadata only: the raw storage "
                + "key carries no extension.", example = "cat.png")
        String filename) {}
