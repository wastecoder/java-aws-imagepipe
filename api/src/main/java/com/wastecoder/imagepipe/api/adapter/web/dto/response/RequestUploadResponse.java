package com.wastecoder.imagepipe.api.adapter.web.dto.response;

import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;
import com.wastecoder.imagepipe.shared.ImageStatus;
import java.util.UUID;

/**
 * Nesting the instructions under {@code upload} separates the durable resource from the one-shot
 * credential that expires, and leaves room for phase 19 to swap in a presigned POST, which carries
 * form fields rather than headers.
 */
public record RequestUploadResponse(UUID uploadId, ImageStatus status, UploadInstructionsResponse upload) {

    public static RequestUploadResponse from(RequestUploadView view) {
        return new RequestUploadResponse(view.uploadId(), view.status(),
                new UploadInstructionsResponse("PUT", view.uploadUrl(),
                        view.requiredHeaders(), view.expiresAt()));
    }
}
