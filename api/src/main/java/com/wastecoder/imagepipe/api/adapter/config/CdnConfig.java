package com.wastecoder.imagepipe.api.adapter.config;

import com.wastecoder.imagepipe.api.adapter.cdn.CloudFrontUrlResolver;
import com.wastecoder.imagepipe.api.application.port.out.CdnUrlResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CdnConfig {

    @Bean
    CdnUrlResolver cdnUrlResolver(ImagepipeProperties properties) {
        return new CloudFrontUrlResolver(properties.cdn().baseUrl());
    }
}
