package com.wastecoder.imagepipe.api.adapter.web;

import com.wastecoder.imagepipe.api.adapter.web.dto.request.RequestUploadRequest;
import com.wastecoder.imagepipe.api.adapter.web.dto.response.RequestUploadResponse;
import com.wastecoder.imagepipe.api.application.port.in.RequestUploadUseCase;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadCommand;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads")
@Tag(name = "Uploads", description = "Requesting permission to upload an image")
public class UploadController {

    private final RequestUploadUseCase requestUploadUseCase;

    public UploadController(RequestUploadUseCase requestUploadUseCase) {
        this.requestUploadUseCase = requestUploadUseCase;
    }

    @Operation(
            summary = "Request an upload and receive a presigned PUT URL",
            description = """
                    Step 1 of 2. The bytes never travel through this API: the response carries a URL
                    signed for S3, and the client uploads straight to it.

                    **Swagger UI cannot perform step 2** — the PUT goes to a different host. Use curl,
                    or the `local/uploads.http` file in this repository.

                    ```bash
                    curl.exe -T "C:/path/to/cat.png" \\
                      -H "Content-Type: image/png" \\
                      "<paste upload.url here>" \\
                      -sS -D - --fail-with-body
                    ```

                    Both the Content-Type and the exact byte count are signed. Send a different media
                    type, or a different number of bytes, and S3 answers 403 SignatureDoesNotMatch.

                    On Windows PowerShell use `curl.exe`, never `curl` — that is an alias for
                    Invoke-WebRequest and every flag above is silently misread. Quote the URL: an
                    unquoted `&` is a parser error.
                    """)
    @PostMapping
    public ResponseEntity<RequestUploadResponse> requestUpload(@Valid @RequestBody RequestUploadRequest request) {
        RequestUploadView view = requestUploadUseCase.requestUpload(
                new RequestUploadCommand(request.contentType(), request.sizeBytes(), request.filename()));

        return ResponseEntity
                .created(URI.create("/images/" + view.uploadId()))
                .body(RequestUploadResponse.from(view));
    }
}
