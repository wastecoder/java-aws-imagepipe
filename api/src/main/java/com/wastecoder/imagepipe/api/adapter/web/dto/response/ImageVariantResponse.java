package com.wastecoder.imagepipe.api.adapter.web.dto.response;

import com.wastecoder.imagepipe.api.application.viewmodel.VariantView;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;

public record ImageVariantResponse(VariantSize size, URI url, int width, int height) {

    public static ImageVariantResponse from(VariantView view) {
        return new ImageVariantResponse(view.size(), view.url(), view.width(), view.height());
    }
}
