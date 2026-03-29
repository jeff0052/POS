package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.PurchaseInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaPurchaseInvoiceRepository extends JpaRepository<PurchaseInvoiceEntity, Long> {
    List<PurchaseInvoiceEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<PurchaseInvoiceEntity> findByStoreIdAndInvoiceStatus(Long storeId, String invoiceStatus);
    boolean existsByStoreIdAndInvoiceNo(Long storeId, String invoiceNo);
}
