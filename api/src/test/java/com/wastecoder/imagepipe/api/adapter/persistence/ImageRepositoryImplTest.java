package com.wastecoder.imagepipe.api.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.wastecoder.imagepipe.api.adapter.persistence.database.ImageJpaDatabase;
import com.wastecoder.imagepipe.api.adapter.persistence.entity.ImageEntity;
import com.wastecoder.imagepipe.api.adapter.persistence.mapper.ImageEntityMapper;
import com.wastecoder.imagepipe.api.domain.model.Image;
import com.wastecoder.imagepipe.api.fixture.ImageMother;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The real round trip through PostgreSQL is {@code ImageRepositoryIntegrationTest}'s job. This test
 * covers the class in isolation: that it wires the mapper and the database together correctly.
 */
@ExtendWith(MockitoExtension.class)
class ImageRepositoryImplTest {

    @Mock
    private ImageJpaDatabase database;

    @Mock
    private ImageEntityMapper mapper;

    private ImageRepositoryImpl repository() {
        return new ImageRepositoryImpl(database, mapper);
    }

    @Test
    @DisplayName("Given an image, when saving it, then it is mapped to an entity, persisted, and mapped back")
    void savesThroughTheMapperAndTheDatabase() {
        Image image = ImageMother.pending();
        ImageEntity entityToPersist = new ImageEntity();
        ImageEntity persistedEntity = new ImageEntity();
        given(mapper.toEntity(image)).willReturn(entityToPersist);
        given(database.save(entityToPersist)).willReturn(persistedEntity);
        given(mapper.toDomain(persistedEntity)).willReturn(image);

        Image result = repository().save(image);

        assertThat(result).isEqualTo(image);
        verify(database).save(entityToPersist);
    }

    @Test
    @DisplayName("Given a known id, when reading an image, then the stored entity is mapped to the domain")
    void findsAKnownImageThroughTheMapper() {
        UUID id = UUID.randomUUID();
        ImageEntity entity = new ImageEntity();
        Image image = ImageMother.pending();
        given(database.findById(id)).willReturn(Optional.of(entity));
        given(mapper.toDomain(entity)).willReturn(image);

        Optional<Image> result = repository().findById(id);

        assertThat(result).contains(image);
    }

    @Test
    @DisplayName("Given an unknown id, when reading an image, then nothing comes back")
    void returnsEmptyForAnUnknownId() {
        UUID id = UUID.randomUUID();
        given(database.findById(id)).willReturn(Optional.empty());

        assertThat(repository().findById(id)).isEmpty();
    }
}
