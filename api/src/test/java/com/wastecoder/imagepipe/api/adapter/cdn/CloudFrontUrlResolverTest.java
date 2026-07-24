package com.wastecoder.imagepipe.api.adapter.cdn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.wastecoder.imagepipe.shared.VariantSize;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CloudFrontUrlResolverTest {

    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    @DisplayName("Given a base url, when resolving a variant, then the deterministic key is appended")
    void appendsTheDerivedKey() {
        CloudFrontUrlResolver resolver = new CloudFrontUrlResolver("https://d111abc.cloudfront.net");

        assertThat(resolver.resolve(ID, VariantSize.SMALL))
                .hasToString("https://d111abc.cloudfront.net/derived/" + ID + "/small.webp");
    }

    @Test
    @DisplayName("Given a base url with a trailing slash, when resolving a variant, then the url has no double slash")
    void stripsATrailingSlash() {
        CloudFrontUrlResolver resolver = new CloudFrontUrlResolver("http://localhost:4566/imagepipe-derived/");

        assertThat(resolver.resolve(ID, VariantSize.LARGE))
                .hasToString("http://localhost:4566/imagepipe-derived/derived/" + ID + "/large.webp");
    }

    @Test
    @DisplayName("Given every variant size, when resolving, then each one gets its own slug")
    void resolvesEverySize() {
        CloudFrontUrlResolver resolver = new CloudFrontUrlResolver("https://cdn");

        assertThat(VariantSize.values())
                .extracting(size -> resolver.resolve(ID, size).toString())
                .containsExactly(
                        "https://cdn/derived/" + ID + "/small.webp",
                        "https://cdn/derived/" + ID + "/medium.webp",
                        "https://cdn/derived/" + ID + "/large.webp");
    }

    @Test
    @DisplayName("Given no base url, when creating the resolver, then it is rejected at startup")
    void rejectsAnAbsentBaseUrl() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CloudFrontUrlResolver(null))
                .withMessageContaining("base-url");
    }
}
