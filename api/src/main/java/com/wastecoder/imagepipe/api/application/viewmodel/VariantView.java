package com.wastecoder.imagepipe.api.application.viewmodel;

import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;

public record VariantView(VariantSize size, URI url, int width, int height) {}
