package com.wastecoder.imagepipe.api.application.port.in;

import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadCommand;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;

/** Driving port: request permission to upload an image and receive the presigned instructions. */
public interface RequestUploadUseCase {

    RequestUploadView requestUpload(RequestUploadCommand command);
}
