package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.PurchaseInvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaPurchaseInvoiceItemRepository extends JpaRepository<PurchaseInvoiceItemEntity, Long> {
    List<PurchaseInvoiceItemEntity> findByInvoiceId(Long invoiceId);
}
