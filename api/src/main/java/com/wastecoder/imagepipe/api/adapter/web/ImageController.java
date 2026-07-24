package com.wastecoder.imagepipe.api.adapter.web;

import com.wastecoder.imagepipe.api.adapter.web.dto.response.ImageResponse;
import com.wastecoder.imagepipe.api.application.port.in.GetImageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@Tag(name = "Images", description = "Reading an image's status and its variants")
public class ImageController {

    private final GetImageUseCase getImageUseCase;

    public ImageController(GetImageUseCase getImageUseCase) {
        this.getImageUseCase = getImageUseCase;
    }

    @Operation(
            summary = "Read an image's status and variant URLs",
            description = """
                    Poll this after step 2 of the upload. The status walks PENDING, then READY once
                    the worker has written every variant, or FAILED if the bytes turned out not to be
                    a usable image.

                    Until phase 2 exists there is nothing to move an image out of PENDING, so
                    `variants` stays empty here by design.
                    """)
    @GetMapping("/{id}")
    public ImageResponse getImage(@PathVariable UUID id) {
        return ImageResponse.from(getImageUseCase.getImage(id));
    }
}
