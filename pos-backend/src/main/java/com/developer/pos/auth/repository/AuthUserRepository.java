package com.developer.pos.auth.repository;

import com.developer.pos.auth.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {
    Optional<AuthUserEntity> findByUsernameAndUserStatus(String username, String userStatus);
    Optional<AuthUserEntity> findByUsername(String username);
}
