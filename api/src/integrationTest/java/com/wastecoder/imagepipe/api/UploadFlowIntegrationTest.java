package com.wastecoder.imagepipe.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import com.wastecoder.imagepipe.shared.ImageStatus;
import com.wastecoder.imagepipe.shared.StorageKeys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The whole of phase 1 in one place: request an upload through the API, then upload the bytes to S3
 * with the URL it handed back, against a real PostgreSQL and a LocalStack that verifies signatures.
 *
 * <p>MockMvc drives the API and a plain HttpClient drives the PUT, because the PUT deliberately does
 * not go through this application — that is the entire point of a presigned URL.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UploadFlowIntegrationTest {

    private static final byte[] BYTES = "pretend this is a png".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("Given an upload request, when uploading with the signed headers, then the object and the row exist")
    void completesTheHappyPath() throws Exception {
        String body = requestUpload(BYTES.length);
        UUID uploadId = UUID.fromString(JsonPath.read(body, "$.uploadId"));

        HttpResponse<String> put = put(body, BYTES, "image/png");

        assertThat(put.statusCode()).isEqualTo(200);
        assertThat(s3.headObject(request -> request
                        .bucket(TestcontainersConfiguration.RAW_BUCKET)
                        .key(StorageKeys.rawKey(uploadId)))
                .contentLength()).isEqualTo(BYTES.length);
        assertThat(imageRepository.findById(uploadId)).get()
                .extracting(Image::status)
                .isEqualTo(ImageStatus.PENDING);
    }

    @Test
    @DisplayName("Given an upload request, when uploading with a different content type, then S3 refuses it")
    void refusesADivergentContentType() throws Exception {
        String body = requestUpload(BYTES.length);

        HttpResponse<String> put = put(body, BYTES, "image/jpeg");

        assertThat(put.statusCode()).isEqualTo(403);
        assertThat(put.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given an upload request, when uploading a different number of bytes, then S3 refuses it")
    void refusesADivergentContentLength() throws Exception {
        String body = requestUpload(BYTES.length);

        HttpResponse<String> put = put(body, "short".getBytes(StandardCharsets.UTF_8), "image/png");

        assertThat(put.statusCode()).isEqualTo(403);
        assertThat(put.body()).contains("SignatureDoesNotMatch");
    }

    @Test
    @DisplayName("Given an unsupported content type, when requesting an upload, then nothing is stored")
    void refusesAnUnsupportedContentTypeBeforeStoringAnything() throws Exception {
        mockMvc.perform(post("/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/zip","sizeBytes":1024}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type")
                        .value("https://imagepipe.wastecoder.com/problems/unsupported-content-type"));
    }

    @Test
    @DisplayName("Given a stored ready image, when reading it, then the cdn urls follow the deterministic keys")
    void readsAReadyImageWithItsCdnUrls() throws Exception {
        Image stored = imageRepository.save(ImageMother.ready());

        mockMvc.perform(get("/images/{id}", stored.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.variants.length()").value(3))
                .andExpect(jsonPath("$.variants[?(@.size == 'SMALL')].url")
                        .value(org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.endsWith("/derived/" + stored.id() + "/small.webp"))));
    }

    @Test
    @DisplayName("Given an unknown id, when reading an image, then it returns 404")
    void reportsAnUnknownImage() throws Exception {
        mockMvc.perform(get("/images/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://imagepipe.wastecoder.com/problems/image-not-found"));
    }

    private String requestUpload(long sizeBytes) throws Exception {
        return mockMvc.perform(post("/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/png\",\"sizeBytes\":" + sizeBytes + ",\"filename\":\"cat.png\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Content-Length is left to the client, as a real one would: java.net.http restricts it. */
    private static HttpResponse<String> put(String responseBody, byte[] bytes, String contentType) throws Exception {
        String url = JsonPath.read(responseBody, "$.upload.url");
        Map<String, String> required = JsonPath.read(responseBody, "$.upload.requiredHeaders");
        assertThat(required).containsKeys("Content-Type", "Content-Length");

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
