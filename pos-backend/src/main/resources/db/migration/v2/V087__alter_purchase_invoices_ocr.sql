-- G06 送货单 OCR: 进货单加 OCR 结果字段
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL COMMENT 'OCR 原始识别结果' AFTER ocr_status,
  ADD COLUMN ocr_confidence DECIMAL(3,2) NULL COMMENT '识别置信度 0-1' AFTER ocr_raw_result,
  ADD COLUMN ocr_reviewed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '人工已复核' AFTER ocr_confidence,
  ADD COLUMN ocr_reviewed_by BIGINT NULL AFTER ocr_reviewed,
  ADD COLUMN ocr_reviewed_at TIMESTAMP NULL AFTER ocr_reviewed_by;
