package com.wastecoder.imagepipe.api.adapter.config;

import com.wastecoder.imagepipe.api.application.port.in.GetImageUseCase;
import com.wastecoder.imagepipe.api.application.port.in.RequestUploadUseCase;
import com.wastecoder.imagepipe.api.application.port.out.CdnUrlResolver;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import com.wastecoder.imagepipe.api.application.usecase.GetImageUseCaseImpl;
import com.wastecoder.imagepipe.api.application.usecase.RequestUploadUseCaseImpl;
import com.wastecoder.imagepipe.api.domain.model.UploadConstraints;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The composition root of the application layer.
 *
 * <p>The use cases carry no Spring annotation on purpose: keeping the annotation here is what lets
 * the inner layers stay framework-free, and it is why they are unit-testable with a plain
 * constructor call.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    /** Injected rather than called statically, so a use case can be tested at a fixed instant. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RequestUploadUseCase requestUploadUseCase(ImageRepository imageRepository,
                                             PresignedUrlGenerator presignedUrlGenerator,
                                             UploadConstraints uploadConstraints,
                                             Clock clock) {

        return new RequestUploadUseCaseImpl(imageRepository, presignedUrlGenerator, uploadConstraints, clock);
    }

    @Bean
    GetImageUseCase getImageUseCase(ImageRepository imageRepository, CdnUrlResolver cdnUrlResolver) {
        return new GetImageUseCaseImpl(imageRepository, cdnUrlResolver);
    }
}
