-- ============================================================
-- V16__seed_data.sql  –  Initial seed: admin, machines, menu
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Unique index để ON CONFLICT hoạt động trên menu_items.name
CREATE UNIQUE INDEX IF NOT EXISTS uq_menu_items_name ON menu_items (name);

-- ── Admin ────────────────────────────────────────────────────────────────────
INSERT INTO users (phone_number, password_hash, full_name, role, is_active)
VALUES ('admin', crypt('admin123', gen_salt('bf', 10)), 'Administrator', 'ADMIN', true)
ON CONFLICT (phone_number) DO NOTHING;

-- ── Máy tính ─────────────────────────────────────────────────────────────────
INSERT INTO machines (machine_code, machine_name, room_zone, status) VALUES
    ('PC-01', 'Máy 1',  'Khu A', 'AVAILABLE'),
    ('PC-02', 'Máy 2',  'Khu A', 'AVAILABLE'),
    ('PC-03', 'Máy 3',  'Khu A', 'AVAILABLE'),
    ('PC-04', 'Máy 4',  'Khu A', 'AVAILABLE'),
    ('PC-05', 'Máy 5',  'Khu A', 'AVAILABLE'),
    ('PC-06', 'Máy 6',  'Khu A', 'AVAILABLE'),
    ('PC-07', 'Máy 7',  'Khu A', 'AVAILABLE'),
    ('PC-08', 'Máy 8',  'Khu A', 'AVAILABLE'),
    ('PC-09', 'Máy 9',  'Khu A', 'AVAILABLE'),
    ('PC-10', 'Máy 10', 'Khu A', 'AVAILABLE')
ON CONFLICT (machine_code) DO NOTHING;

-- ── Menu: Mì / Cơm ───────────────────────────────────────────────────────────
INSERT INTO menu_items (name, price, category, is_available) VALUES
    ('Mì gói',          15000, 'Mì/Cơm', true),
    ('Mì trứng',        20000, 'Mì/Cơm', true),
    ('Mì xúc xích',     22000, 'Mì/Cơm', true),
    ('Mì bò',           25000, 'Mì/Cơm', true),
    ('Mì tôm',          20000, 'Mì/Cơm', true),
    ('Mì xào',          30000, 'Mì/Cơm', true),
    ('Cơm chiên trứng', 25000, 'Mì/Cơm', true),
    ('Cơm bò xào',      30000, 'Mì/Cơm', true)
ON CONFLICT (name) DO NOTHING;

-- Addons cho toàn bộ món Mì/Cơm (idempotent)
INSERT INTO menu_item_addons (menu_item_id, name, extra_price, is_available)
SELECT mi.id, a.name, a.extra_price, true
FROM menu_items mi
CROSS JOIN (VALUES
    ('Thêm trứng',     7000),
    ('Thêm xúc xích',  8000),
    ('Thêm chả',       8000),
    ('Thêm tôm',      10000),
    ('Thêm gói mì',    8000),
    ('Thêm rau',       3000)
) AS a(name, extra_price)
WHERE mi.category = 'Mì/Cơm'
  AND NOT EXISTS (
      SELECT 1 FROM menu_item_addons mia
      WHERE mia.menu_item_id = mi.id AND mia.name = a.name
  );

-- ── Menu: Đồ ăn vặt ──────────────────────────────────────────────────────────
INSERT INTO menu_items (name, price, category, is_available) VALUES
    ('Cá viên chiên',   15000, 'Đồ ăn vặt', true),
    ('Khoai tây chiên', 25000, 'Đồ ăn vặt', true),
    ('Xúc xích chiên',  15000, 'Đồ ăn vặt', true),
    ('Bánh tráng trộn', 15000, 'Đồ ăn vặt', true),
    ('Snack Oishi',     10000, 'Đồ ăn vặt', true),
    ('Snack Poca',      10000, 'Đồ ăn vặt', true),
    ('Bánh quy',        10000, 'Đồ ăn vặt', true),
    ('Hạt hướng dương', 10000, 'Đồ ăn vặt', true),
    ('Kẹo singum',       5000, 'Đồ ăn vặt', true)
ON CONFLICT (name) DO NOTHING;

-- ── Menu: Nước ngọt ──────────────────────────────────────────────────────────
INSERT INTO menu_items (name, price, category, is_available) VALUES
    ('Pepsi lon',               15000, 'Nước ngọt', true),
    ('Coca-Cola lon',           15000, 'Nước ngọt', true),
    ('7UP lon',                 15000, 'Nước ngọt', true),
    ('Sting lon',               15000, 'Nước ngọt', true),
    ('Mirinda lon',             15000, 'Nước ngọt', true),
    ('Trà xanh C2',             15000, 'Nước ngọt', true),
    ('Aquarius',                15000, 'Nước ngọt', true),
    ('Nước suối',                8000, 'Nước ngọt', true),
    ('Redbull lon',             25000, 'Nước ngọt', true),
    ('Number One lon',          15000, 'Nước ngọt', true),
    ('Nước tăng lực Warrior',   20000, 'Nước ngọt', true)
ON CONFLICT (name) DO NOTHING;

-- ── Menu: Cà phê ─────────────────────────────────────────────────────────────
INSERT INTO menu_items (name, price, category, is_available) VALUES
    ('Cà phê đen nóng',      15000, 'Cà phê', true),
    ('Cà phê sữa nóng',      20000, 'Cà phê', true),
    ('Bạc xỉu nóng',         20000, 'Cà phê', true),
    ('Cà phê đen đá',        15000, 'Cà phê', true),
    ('Cà phê sữa đá',        20000, 'Cà phê', true),
    ('Bạc xỉu đá',           20000, 'Cà phê', true),
    ('Cà phê lon Highlands',  25000, 'Cà phê', true),
    ('Cà phê lon Nestlé',    20000, 'Cà phê', true)
ON CONFLICT (name) DO NOTHING;

-- ── Menu: Trà sữa ────────────────────────────────────────────────────────────
INSERT INTO menu_items (name, price, category, is_available) VALUES
    ('Trà sữa trân châu', 30000, 'Trà sữa', true),
    ('Trà sữa thạch',     32000, 'Trà sữa', true),
    ('Matcha sữa',        35000, 'Trà sữa', true),
    ('Trà đào cam sả',    30000, 'Trà sữa', true),
    ('Trà vải',           28000, 'Trà sữa', true),
    ('Trà chanh',         20000, 'Trà sữa', true)
ON CONFLICT (name) DO NOTHING;
