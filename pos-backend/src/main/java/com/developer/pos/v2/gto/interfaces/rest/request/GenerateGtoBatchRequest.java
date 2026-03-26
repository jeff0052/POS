package com.developer.pos.v2.gto.interfaces.rest.request;

import java.time.LocalDate;

public class GenerateGtoBatchRequest {

    private Long merchantId;
    private Long storeId;
    private LocalDate exportDate;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public LocalDate getExportDate() {
        return exportDate;
    }

    public void setExportDate(LocalDate exportDate) {
        this.exportDate = exportDate;
    }
}
