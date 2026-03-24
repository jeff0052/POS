package com.developer.pos.order.repository;

import com.developer.pos.order.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findTop50ByStoreIdOrderByCreatedAtDesc(Long storeId);

    OrderEntity findTopByOrderByIdDesc();

    Optional<OrderEntity> findByOrderNo(String orderNo);
}
