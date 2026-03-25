package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaStoreLookupRepository extends JpaRepository<StoreEntity, Long> {
    @Query(
            value = """
                    select
                        s.id as id,
                        0 as merchant_id,
                        s.store_code as store_code,
                        s.store_name as store_name
                    from stores s
                    where s.store_code = ?1
                    """,
            nativeQuery = true
    )
    Optional<StoreEntity> findByStoreCode(String storeCode);
}
