package com.wastecoder.imagepipe.api.adapter.persistence.database;

import com.wastecoder.imagepipe.api.adapter.persistence.entity.ImageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data's view of the images table. Named database, not repository: the port owns that word. */
public interface ImageJpaDatabase extends JpaRepository<ImageEntity, UUID> {}
