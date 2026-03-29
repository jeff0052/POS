-- V087: 送货单加 OCR 原始结果
-- Journey: J08 库存
-- 注意: ocr_status 和 scan_image_url 已存在（doc/66），只加 ocr_raw_result
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL AFTER ocr_status;
