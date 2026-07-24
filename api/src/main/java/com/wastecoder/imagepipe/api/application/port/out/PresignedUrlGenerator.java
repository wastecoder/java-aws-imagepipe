package com.wastecoder.imagepipe.api.application.port.out;

import java.util.UUID;

/** Driven port: issues a short-lived, single-purpose upload credential. */
public interface PresignedUrlGenerator {

    PresignedUpload generate(UUID uploadId, String contentType, long sizeBytes);
}
