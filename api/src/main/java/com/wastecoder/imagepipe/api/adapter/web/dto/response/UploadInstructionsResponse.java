package com.wastecoder.imagepipe.api.adapter.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record UploadInstructionsResponse(

        @Schema(description = "Always PUT in this phase.", example = "PUT")
        String method,

        @Schema(description = "Absolute URL to send the raw bytes to. This is S3, not this API, so "
                + "Swagger UI cannot perform this step — use the curl example on the operation.")
        URI url,

        @Schema(description = "Headers that must be sent verbatim. They were signed, and a request "
                + "missing or altering one of them is refused with 403 SignatureDoesNotMatch.")
        Map<String, String> requiredHeaders,

        @Schema(description = "After this instant the URL is refused with 403 AccessDenied.")
        Instant expiresAt) {}
