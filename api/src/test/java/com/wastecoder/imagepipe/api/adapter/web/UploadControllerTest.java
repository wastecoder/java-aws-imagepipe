package com.wastecoder.imagepipe.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wastecoder.imagepipe.api.application.port.in.RequestUploadUseCase;
import com.wastecoder.imagepipe.api.application.viewmodel.RequestUploadView;
import com.wastecoder.imagepipe.api.domain.exception.ImageTooLargeException;
import com.wastecoder.imagepipe.api.domain.exception.UnsupportedContentTypeException;
import com.wastecoder.imagepipe.shared.ImageStatus;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The advice is not imported: @WebMvcTest scans @ControllerAdvice, so GlobalExceptionHandler is
 * inside the slice and its Problem Details output is directly assertable here.
 */
@WebMvcTest(UploadController.class)
class UploadControllerTest {

    private static final UUID UPLOAD_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String VALID_BODY = """
            {"contentType":"image/png","sizeBytes":204813,"filename":"cat.png"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestUploadUseCase requestUploadUseCase;

    @Test
    @DisplayName("Given a valid request, when posting an upload, then it returns 201 with the signed instructions")
    void returnsTheSignedInstructions() throws Exception {
        given(requestUploadUseCase.requestUpload(any())).willReturn(new RequestUploadView(
                UPLOAD_ID,
                ImageStatus.PENDING,
                URI.create("http://localhost:4566/imagepipe-raw/uploads/" + UPLOAD_ID + "?X-Amz-Signature=abc"),
                Map.of("Content-Type", "image/png", "Content-Length", "204813"),
                Instant.parse("2026-07-24T18:05:00Z")));

        mockMvc.perform(post("/uploads").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/images/" + UPLOAD_ID))
                .andExpect(jsonPath("$.uploadId").value(UPLOAD_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.upload.method").value("PUT"))
                .andExpect(jsonPath("$.upload.url")
                        .value("http://localhost:4566/imagepipe-raw/uploads/" + UPLOAD_ID + "?X-Amz-Signature=abc"))
                .andExpect(jsonPath("$.upload.requiredHeaders['Content-Type']").value("image/png"))
                .andExpect(jsonPath("$.upload.requiredHeaders['Content-Length']").value("204813"))
                .andExpect(jsonPath("$.upload.expiresAt").value("2026-07-24T18:05:00Z"));
    }

    @Test
    @DisplayName("Given a blank content type, when posting an upload, then it returns 400 with the violation")
    void refusesABlankContentType() throws Exception {
        mockMvc.perform(post("/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"  ","sizeBytes":204813}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.type").value("https://imagepipe.wastecoder.com/problems/invalid-payload"))
                .andExpect(jsonPath("$.title").value("Invalid payload"))
                .andExpect(jsonPath("$.violations[0].field").value("contentType"));
    }

    @Test
    @DisplayName("Given a non positive size, when posting an upload, then it returns 400 with the violation")
    void refusesANonPositiveSize() throws Exception {
        mockMvc.perform(post("/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/png","sizeBytes":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("sizeBytes"));
    }

    @Test
    @DisplayName("Given an unsupported content type, when posting an upload, then it returns 422 with the allowlist")
    void refusesAnUnsupportedContentType() throws Exception {
        willThrow(new UnsupportedContentTypeException("application/zip", Set.of("image/png", "image/jpeg")))
                .given(requestUploadUseCase).requestUpload(any());

        mockMvc.perform(post("/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/zip","sizeBytes":204813}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.type")
                        .value("https://imagepipe.wastecoder.com/problems/unsupported-content-type"))
                .andExpect(jsonPath("$.title").value("Unsupported content type"))
                .andExpect(jsonPath("$.contentType").value("application/zip"))
                .andExpect(jsonPath("$.allowedContentTypes").isArray());
    }

    @Test
    @DisplayName("Given a size above the ceiling, when posting an upload, then it returns 422 with both sizes")
    void refusesAnOversizedUpload() throws Exception {
        willThrow(new ImageTooLargeException(20_000_000L, 10_485_760L))
                .given(requestUploadUseCase).requestUpload(any());

        mockMvc.perform(post("/uploads").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("https://imagepipe.wastecoder.com/problems/image-too-large"))
                .andExpect(jsonPath("$.sizeBytes").value(20_000_000L))
                .andExpect(jsonPath("$.maxSizeBytes").value(10_485_760L));
    }
}
