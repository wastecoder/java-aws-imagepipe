package com.wastecoder.imagepipe.api.application.viewmodel;

import com.wastecoder.imagepipe.shared.ImageStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageView(UUID id, ImageStatus status, String contentType, long sizeBytes,
                        String filename, Instant createdAt, List<VariantView> variants) {}
