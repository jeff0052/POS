package com.developer.pos.v2.mcp.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaActionLogRepository extends JpaRepository<ActionLogEntity, Long> {
    List<ActionLogEntity> findByToolNameOrderByCreatedAtDesc(String toolName);
    List<ActionLogEntity> findByActorTypeOrderByCreatedAtDesc(String actorType);
}
