-- Tự động liên kết inventory_items → menu_items theo tên giống nhau (case-insensitive)
-- Sau bước này, khi giao đơn hàng hệ thống sẽ tự động trừ kho
UPDATE inventory_items ii
SET menu_item_id = mi.id
FROM menu_items mi
WHERE lower(ii.name) = lower(mi.name)
  AND ii.menu_item_id IS NULL;
