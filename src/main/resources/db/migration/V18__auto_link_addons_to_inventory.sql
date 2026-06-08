-- Tự động liên kết addon "Thêm X" → inventory item "X" theo tên khớp
-- VD: "Thêm xúc xích" → inventory "Xúc xích" (id=3)
--     "Thêm trứng"    → inventory "Trứng"    (id=2)
--     "Thêm gói mì"   → inventory "Mì gói"   (id=1)
UPDATE menu_item_addons mia
SET inventory_item_id = ii.id
FROM inventory_items ii
WHERE lower(mia.name) = lower('Thêm ' || ii.name)
  AND mia.inventory_item_id IS NULL;

-- Liên kết "Xúc xích chiên" menu item → "Xúc xích" inventory qua junction table
INSERT INTO menu_item_inventory (menu_item_id, inventory_item_id)
SELECT mi.id, ii.id
FROM menu_items mi
CROSS JOIN inventory_items ii
WHERE lower(mi.name) = lower('Xúc xích chiên')
  AND lower(ii.name) = lower('Xúc xích')
ON CONFLICT DO NOTHING;
