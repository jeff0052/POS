package com.developer.pos.v2.image.infrastructure.persistence.repository;

import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaImageAssetRepository extends JpaRepository<ImageAssetEntity, Long> {

    Optional<ImageAssetEntity> findByImageIdAndStatus(String imageId, String status);

    Optional<ImageAssetEntity> findByImageId(String imageId);

    List<ImageAssetEntity> findByImageIdInAndStatus(Collection<String> imageIds, String status);
}
