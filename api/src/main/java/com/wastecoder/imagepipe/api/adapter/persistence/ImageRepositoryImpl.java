package com.wastecoder.imagepipe.api.adapter.persistence;

import com.wastecoder.imagepipe.api.adapter.persistence.database.ImageJpaDatabase;
import com.wastecoder.imagepipe.api.adapter.persistence.mapper.ImageEntityMapper;
import com.wastecoder.imagepipe.api.application.port.out.ImageRepository;
import com.wastecoder.imagepipe.api.domain.model.Image;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ImageRepositoryImpl implements ImageRepository {

    private final ImageJpaDatabase database;
    private final ImageEntityMapper mapper;

    public ImageRepositoryImpl(ImageJpaDatabase database, ImageEntityMapper mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    @Override
    public Image save(Image image) {
        return mapper.toDomain(database.save(mapper.toEntity(image)));
    }

    @Override
    public Optional<Image> findById(UUID id) {
        return database.findById(id).map(mapper::toDomain);
    }
}
