package com.developer.pos.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_settings")
public class StoreSettingsEntity {

    @Id
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "receipt_title")
    private String receiptTitle;

    @Column(name = "receipt_footer")
    private String receiptFooter;

    @Column(name = "printer_config_json")
    private String printerConfigJson;

    @Column(name = "payment_config_json")
    private String paymentConfigJson;

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getReceiptTitle() {
        return receiptTitle;
    }

    public String getReceiptFooter() {
        return receiptFooter;
    }

    public String getPrinterConfigJson() {
        return printerConfigJson;
    }

    public String getPaymentConfigJson() {
        return paymentConfigJson;
    }
}
