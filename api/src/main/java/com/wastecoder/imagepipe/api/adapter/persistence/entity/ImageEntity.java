package com.wastecoder.imagepipe.api.adapter.persistence.entity;

import com.wastecoder.imagepipe.shared.ImageStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lombok appears here and only here. JPA demands a no-argument constructor and accessors, and this
 * is exactly the code the coverage gates already exclude. The aggregate itself is written by hand.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
public class ImageEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImageStatus status;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(length = 255)
    private String filename;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Eager on purpose: there are at most three rows, GET /images/{id} always wants them, and lazy
     * loading here would only buy a LazyInitializationException outside the transaction.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "image_variants", joinColumns = @JoinColumn(name = "image_id"))
    private List<ImageVariantEntity> variants = new ArrayList<>();
}
