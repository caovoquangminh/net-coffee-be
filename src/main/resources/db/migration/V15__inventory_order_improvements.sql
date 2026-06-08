-- Link addon topping → inventory item (tuỳ chọn, nullable)
-- Khi tồn kho của nguyên liệu = 0, addon liên kết sẽ tự động bị vô hiệu hoá
ALTER TABLE menu_item_addons
    ADD COLUMN inventory_item_id BIGINT REFERENCES inventory_items(id);

CREATE INDEX idx_addon_inventory_item_id ON menu_item_addons(inventory_item_id);

-- Xác nhận thanh toán cho đơn chuyển khoản
-- CASH: luôn = true  |  BANK_TRANSFER: bắt đầu = false, nhân viên phải xác nhận thủ công
ALTER TABLE orders
    ADD COLUMN payment_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Đơn BANK_TRANSFER hiện có: đặt về FALSE để nhất quán
-- (đơn cũ đã ở trạng thái DONE / CANCELLED thì không bị ảnh hưởng vì staff không thể thay đổi chúng)
UPDATE orders SET payment_verified = FALSE WHERE payment_method = 'BANK_TRANSFER';
