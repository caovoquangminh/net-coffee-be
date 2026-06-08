-- Clear all menu items and related order data for fresh test setup
TRUNCATE TABLE
    order_item_addons,
    order_items,
    orders,
    menu_item_addons,
    menu_item_inventory,
    menu_items
RESTART IDENTITY CASCADE;
