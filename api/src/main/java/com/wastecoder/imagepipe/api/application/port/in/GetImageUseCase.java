package com.wastecoder.imagepipe.api.application.port.in;

import com.wastecoder.imagepipe.api.application.viewmodel.ImageView;
import java.util.UUID;

/** Driving port: read an image's status and, once ready, the URLs of its variants. */
public interface GetImageUseCase {

    ImageView getImage(UUID imageId);
}
