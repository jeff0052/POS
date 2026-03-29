package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2MenuTimeSlotProductEntity")
@Table(name = "menu_time_slot_products")
public class MenuTimeSlotProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time_slot_id", nullable = false)
    private Long timeSlotId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "is_visible")
    private boolean visible;

    protected MenuTimeSlotProductEntity() {
    }

    public MenuTimeSlotProductEntity(Long timeSlotId, Long productId, boolean visible) {
        this.timeSlotId = timeSlotId;
        this.productId = productId;
        this.visible = visible;
    }

    public Long getId() { return id; }
    public Long getTimeSlotId() { return timeSlotId; }
    public Long getProductId() { return productId; }
    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
