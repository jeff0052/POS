package com.developer.pos.v2.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_tables")
public class StoreTableEntity {

    @Id
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_code", nullable = false)
    private String tableCode;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "table_status", nullable = false)
    private String tableStatus;

    protected StoreTableEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getTableCode() {
        return tableCode;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }
}
