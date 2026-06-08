-- Track whether menu items/addons were disabled by the system (stock = 0) vs manually by admin.
-- Without this flag, restocking re-enables everything linked to that inventory item,
-- including items the admin intentionally disabled for unrelated reasons.
ALTER TABLE menu_items
    ADD COLUMN disabled_by_stock BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE menu_item_addons
    ADD COLUMN disabled_by_stock BOOLEAN NOT NULL DEFAULT FALSE;
