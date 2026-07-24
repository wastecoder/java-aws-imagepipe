package com.wastecoder.imagepipe.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root of the API.
 *
 * <p>It sits in the module root package, not under {@code adapter.config}, because that is where
 * component scanning reaches everything below it.
 */
@SpringBootApplication
public class ImagepipeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImagepipeApplication.class, args);
    }
}
