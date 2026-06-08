-- Tạo bảng junction nhiều-nhiều giữa menu_items và inventory_items
-- Cho phép một nguyên liệu (VD: Xúc xích) liên kết với nhiều món ăn
CREATE TABLE menu_item_inventory (
    menu_item_id      BIGINT NOT NULL REFERENCES menu_items(id)      ON DELETE CASCADE,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    PRIMARY KEY (menu_item_id, inventory_item_id)
);

-- Di chuyển dữ liệu liên kết cũ từ inventory_items.menu_item_id sang junction table
INSERT INTO menu_item_inventory (menu_item_id, inventory_item_id)
SELECT menu_item_id, id
FROM inventory_items
WHERE menu_item_id IS NOT NULL;

-- Xóa cột cũ (đã được thay thế bởi junction table)
ALTER TABLE inventory_items DROP COLUMN menu_item_id;
