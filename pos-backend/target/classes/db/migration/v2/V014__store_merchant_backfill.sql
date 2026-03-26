SET @add_merchant_id_column = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE stores ADD COLUMN merchant_id BIGINT NULL',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'stores'
      AND column_name = 'merchant_id'
);

PREPARE add_merchant_id_column_stmt FROM @add_merchant_id_column;
EXECUTE add_merchant_id_column_stmt;
DEALLOCATE PREPARE add_merchant_id_column_stmt;

UPDATE stores
SET merchant_id = 1
WHERE merchant_id IS NULL;
