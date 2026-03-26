package com.developer.pos.v2.ai.recommendation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaAiRecommendationRepository extends JpaRepository<AiRecommendationEntity, Long> {
    Optional<AiRecommendationEntity> findByRecommendationId(String recommendationId);
    List<AiRecommendationEntity> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, String status);
    Page<AiRecommendationEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    List<AiRecommendationEntity> findByStoreIdAndAdvisorRoleAndStatusOrderByCreatedAtDesc(Long storeId, String advisorRole, String status);
}
