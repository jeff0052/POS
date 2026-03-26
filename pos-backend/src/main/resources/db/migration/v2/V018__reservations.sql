CREATE TABLE reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    guest_name VARCHAR(120) NOT NULL,
    reservation_time VARCHAR(16) NOT NULL,
    party_size INT NOT NULL,
    reservation_status VARCHAR(32) NOT NULL,
    area VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (reservation_no),
    KEY idx_reservation_store_status (store_id, reservation_status),
    KEY idx_reservation_store_table (store_id, table_id)
);
