package com.wastecoder.imagepipe.api.adapter.cdn;

import com.wastecoder.imagepipe.api.application.port.out.CdnUrlResolver;
import com.wastecoder.imagepipe.shared.StorageKeys;
import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds the public URL of a variant by appending the deterministic key to a configured base.
 *
 * <p>Named after CloudFront even though phase 1 points it at LocalStack, because CloudFront is what
 * it will be. Phase 7 changes one property and no code: the key is derived, not stored.
 */
public class CloudFrontUrlResolver implements CdnUrlResolver {

    private final String baseUrl;

    public CloudFrontUrlResolver(String baseUrl) {
        Objects.requireNonNull(baseUrl, "imagepipe.cdn.base-url must be configured");
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public URI resolve(UUID uploadId, VariantSize size) {
        return URI.create(baseUrl + "/" + StorageKeys.derivedKey(uploadId, size));
    }
}
