package com.wastecoder.imagepipe.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ImagepipeApplicationIntegrationTest {

    @Test
    @DisplayName("Given the api module, when the context starts against a real postgres, then it loads")
    void contextLoads() {
    }
}
