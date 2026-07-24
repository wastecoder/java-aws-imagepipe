package com.wastecoder.imagepipe.api.adapter.config;

import com.wastecoder.imagepipe.api.domain.model.UploadConstraints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Turns configuration values into the domain rule that uses them. */
@Configuration(proxyBeanMethods = false)
public class UploadConstraintsConfig {

    @Bean
    UploadConstraints uploadConstraints(ImagepipeProperties properties) {
        return new UploadConstraints(properties.upload().allowedContentTypes(),
                properties.upload().maxSizeBytes());
    }
}
