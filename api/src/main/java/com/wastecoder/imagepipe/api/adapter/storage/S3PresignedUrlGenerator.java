package com.wastecoder.imagepipe.api.adapter.storage;

import com.wastecoder.imagepipe.api.application.port.out.PresignedUpload;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import com.wastecoder.imagepipe.shared.StorageKeys;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Issues a presigned PUT for the raw bucket.
 *
 * <p>Both the content type and the content length are signed. That makes the URL usable for exactly
 * one upload, of exactly one media type, of exactly the number of bytes the API already approved.
 *
 * <p>The limit is worth stating plainly: this is an exact-size lock, never a ceiling. SigV4 only
 * matches exactly, so there is no way to express a range with a presigned PUT — that needs a POST
 * policy with content-length-range, which the AWS SDK for Java v2 cannot generate.
 *
 * <p>The other limit, equally worth stating: the signed Content-Type constrains the header, never
 * the bytes. S3 does not inspect the body, so a signed {@code image/png} can be a ZIP, a
 * decompression bomb, or an SVG carrying script. The worker must verify magic bytes itself and
 * treat a mismatch as a permanent failure.
 */
public class S3PresignedUrlGenerator implements PresignedUrlGenerator {

    private static final String HOST_HEADER = "host";

    private final S3Presigner presigner;
    private final String rawBucket;
    private final Duration expiration;

    public S3PresignedUrlGenerator(S3Presigner presigner, String rawBucket, Duration expiration) {
        this.presigner = presigner;
        this.rawBucket = rawBucket;
        this.expiration = expiration;
    }

    @Override
    public PresignedUpload generate(UUID uploadId, String contentType, long sizeBytes) {
        PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(rawBucket)
                        .key(StorageKeys.rawKey(uploadId))
                        .contentType(contentType)
                        .contentLength(sizeBytes)
                        .build())
                .build());

        return new PresignedUpload(
                URI.create(presigned.url().toExternalForm()),
                requiredHeaders(presigned),
                presigned.expiration());
    }

    /**
     * The headers the client must echo, taken from what was actually signed rather than rebuilt by
     * hand.
     *
     * <p>Every field set on the PutObjectRequest becomes a signed header — metadata, tagging and
     * checksums included — and a request missing one of them is refused with 403. Deriving the map
     * means the documented contract cannot drift away from the signature when someone adds a field
     * here later.
     *
     * <p>Host is dropped: every HTTP client sets it itself, and it is not something a caller can act
     * on.
     */
    private static Map<String, String> requiredHeaders(PresignedPutObjectRequest presigned) {
        return presigned.signedHeaders().entrySet().stream()
                .filter(header -> !HOST_HEADER.equalsIgnoreCase(header.getKey()))
                .filter(header -> !header.getValue().isEmpty())
                .collect(Collectors.toMap(
                        header -> conventionalCase(header.getKey()),
                        header -> header.getValue().getFirst(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    /** SigV4 lower-cases header names. Header names are case-insensitive, but callers read better. */
    private static String conventionalCase(String headerName) {
        return switch (headerName.toLowerCase(Locale.ROOT)) {
            case "content-type" -> "Content-Type";
            case "content-length" -> "Content-Length";
            default -> headerName;
        };
    }
}
