DELETE FROM menu_item_addons WHERE menu_item_id IN (SELECT id FROM menu_items WHERE name = 'Cháo cá');
DELETE FROM menu_items WHERE name = 'Cháo cá';
