package com.wastecoder.imagepipe.api.adapter.config;

import com.wastecoder.imagepipe.api.adapter.storage.S3PresignedUrlGenerator;
import com.wastecoder.imagepipe.api.application.port.out.PresignedUrlGenerator;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class AwsS3Config {

    /**
     * One presigner per application. It is annotated thread safe and immutable, and its own javadoc
     * says to build it at startup because building one is expensive. It implements AutoCloseable, so
     * Spring closes it at context shutdown — never wrap it per request.
     */
    @Bean
    S3Presigner s3Presigner(ImagepipeProperties properties) {
        ImagepipeProperties.Aws aws = properties.aws();

        // The region is mandatory even with an endpoint override: it forms the SigV4 credential
        // scope, X-Amz-Credential=.../us-east-1/s3/aws4_request.
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(aws.region()))
                .credentialsProvider(credentialsProvider(aws));

        if (StringUtils.hasText(aws.endpoint())) {
            builder.endpointOverride(URI.create(aws.endpoint()))
                    // S3Presigner.Builder has no forcePathStyle; that lives on S3ClientBuilder only.
                    // Without path style the URL comes out as http://<bucket>.localhost:4566/...,
                    // a host that does not resolve.
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        return builder.build();
    }

    @Bean
    PresignedUrlGenerator presignedUrlGenerator(S3Presigner presigner, ImagepipeProperties properties) {
        return new S3PresignedUrlGenerator(presigner, properties.aws().rawBucket(),
                properties.upload().urlExpiration());
    }

    /**
     * Static credentials locally and in tests, the task role everywhere else. The local secret key
     * has to be exactly "test": LocalStack can only verify a presigned signature produced with that
     * key, and any other value fails every request in a way that reads like a signing bug.
     */
    private static AwsCredentialsProvider credentialsProvider(ImagepipeProperties.Aws aws) {
        if (StringUtils.hasText(aws.accessKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(aws.accessKey(), aws.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
