package com.wastecoder.imagepipe.api.application.port.out;

import com.wastecoder.imagepipe.api.domain.model.Image;
import java.util.Optional;
import java.util.UUID;

/** Driven port: where the aggregate is kept. Named after the capability, not after JPA. */
public interface ImageRepository {

    Image save(Image image);

    Optional<Image> findById(UUID id);
}
