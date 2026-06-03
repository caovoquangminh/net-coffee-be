-- Thêm cột confirmed_by, confirmed_at vào orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS confirmed_by BIGINT,
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;

-- Addon cho menu item
CREATE TABLE IF NOT EXISTS menu_item_addons (
    id             BIGSERIAL PRIMARY KEY,
    menu_item_id   BIGINT        NOT NULL REFERENCES menu_items(id),
    name           VARCHAR(100)  NOT NULL,
    extra_price    NUMERIC(10,2) NOT NULL,
    is_available   BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_addons_menu_item_id ON menu_item_addons(menu_item_id);

-- Addon đã chọn trong từng order item
CREATE TABLE IF NOT EXISTS order_item_addons (
    id            BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT        NOT NULL REFERENCES order_items(id),
    addon_id      BIGINT        NOT NULL,
    addon_name    VARCHAR(100)  NOT NULL,
    addon_price   NUMERIC(10,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_order_item_addons_order_item_id ON order_item_addons(order_item_id);

-- Nhà cung cấp
CREATE TABLE IF NOT EXISTS suppliers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    address    VARCHAR(300),
    note       VARCHAR(500),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Mặt hàng kho
CREATE TABLE IF NOT EXISTS inventory_items (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150)   NOT NULL,
    unit           VARCHAR(30)    NOT NULL,
    current_stock  NUMERIC(12,3)  NOT NULL DEFAULT 0,
    min_stock      NUMERIC(12,3)  NOT NULL DEFAULT 0,
    menu_item_id   BIGINT         REFERENCES menu_items(id),
    description    VARCHAR(500),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Phiếu nhập/xuất kho
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    inventory_item_id  BIGINT        NOT NULL REFERENCES inventory_items(id),
    supplier_id        BIGINT        REFERENCES suppliers(id),
    type               VARCHAR(20)   NOT NULL,
    quantity           NUMERIC(12,3) NOT NULL,
    purchase_price     NUMERIC(15,2),
    expiry_date        DATE,
    notes              VARCHAR(500),
    performed_by       BIGINT        NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_inv_tx_item_id   ON inventory_transactions(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_inv_tx_type      ON inventory_transactions(type);
CREATE INDEX IF NOT EXISTS idx_inv_tx_created_at ON inventory_transactions(created_at);
