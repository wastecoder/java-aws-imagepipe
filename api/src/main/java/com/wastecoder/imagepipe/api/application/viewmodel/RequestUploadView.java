package com.wastecoder.imagepipe.api.application.viewmodel;

import com.wastecoder.imagepipe.shared.ImageStatus;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RequestUploadView(UUID uploadId, ImageStatus status, URI uploadUrl,
                                Map<String, String> requiredHeaders, Instant expiresAt) {}
