package com.wastecoder.imagepipe.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wastecoder.imagepipe.api.application.port.in.GetImageUseCase;
import com.wastecoder.imagepipe.api.application.viewmodel.ImageView;
import com.wastecoder.imagepipe.api.application.viewmodel.VariantView;
import com.wastecoder.imagepipe.api.domain.exception.ImageNotFoundException;
import com.wastecoder.imagepipe.shared.ImageStatus;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    private static final UUID IMAGE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant CREATED_AT = Instant.parse("2026-07-24T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetImageUseCase getImageUseCase;

    @Test
    @DisplayName("Given a pending image, when reading it, then it returns 200 with an empty variant list")
    void returnsAPendingImage() throws Exception {
        given(getImageUseCase.getImage(IMAGE_ID)).willReturn(new ImageView(
                IMAGE_ID, ImageStatus.PENDING, "image/png", 204_813L, "cat.png", CREATED_AT, List.of()));

        mockMvc.perform(get("/images/{id}", IMAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(IMAGE_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(204_813L))
                .andExpect(jsonPath("$.filename").value("cat.png"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-24T18:00:00Z"))
                .andExpect(jsonPath("$.variants").isEmpty());
    }

    @Test
    @DisplayName("Given a ready image, when reading it, then every variant carries its cdn url")
    void returnsAReadyImageWithVariants() throws Exception {
        given(getImageUseCase.getImage(IMAGE_ID)).willReturn(new ImageView(
                IMAGE_ID, ImageStatus.READY, "image/png", 204_813L, "cat.png", CREATED_AT,
                List.of(new VariantView(VariantSize.SMALL, URI.create("https://cdn/small.webp"), 320, 180))));

        mockMvc.perform(get("/images/{id}", IMAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.variants[0].size").value("SMALL"))
                .andExpect(jsonPath("$.variants[0].url").value("https://cdn/small.webp"))
                .andExpect(jsonPath("$.variants[0].width").value(320))
                .andExpect(jsonPath("$.variants[0].height").value(180));
    }

    @Test
    @DisplayName("Given an unknown id, when reading an image, then it returns 404 as problem details")
    void returnsNotFound() throws Exception {
        willThrow(new ImageNotFoundException(IMAGE_ID)).given(getImageUseCase).getImage(any());

        mockMvc.perform(get("/images/{id}", IMAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.type").value("https://imagepipe.wastecoder.com/problems/image-not-found"))
                .andExpect(jsonPath("$.title").value("Image not found"))
                .andExpect(jsonPath("$.imageId").value(IMAGE_ID.toString()));
    }
}
