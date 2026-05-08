-- ============================================================
-- V1__init_schema.sql
-- Net Coffee Management System - Initial Schema
-- ============================================================

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    phone_number     VARCHAR(15)    NOT NULL UNIQUE,
    password_hash    VARCHAR(255)   NOT NULL,
    full_name        VARCHAR(100),
    avatar_url       TEXT,
    balance          NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_spent      NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    version          BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT chk_users_balance_non_negative    CHECK (balance >= 0),
    CONSTRAINT chk_users_total_spent_non_negative CHECK (total_spent >= 0)
);

CREATE INDEX idx_users_phone_number ON users (phone_number);
CREATE INDEX idx_users_is_active    ON users (is_active);

-- ============================================================
-- MACHINES
-- ============================================================
CREATE TABLE machines (
    id                 BIGSERIAL PRIMARY KEY,
    machine_code       VARCHAR(20)  NOT NULL UNIQUE,
    machine_name       VARCHAR(100) NOT NULL,
    room_zone          VARCHAR(50),
    status             VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    current_session_id BIGINT,
    ip_address         VARCHAR(45),
    mac_address        VARCHAR(17),
    specs              TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_machines_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'LOCKED', 'MAINTENANCE'))
);

CREATE INDEX idx_machines_status       ON machines (status);
CREATE INDEX idx_machines_machine_code ON machines (machine_code);

-- ============================================================
-- PRICE PLANS
-- ============================================================
CREATE TABLE price_plans (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    price_per_hour   NUMERIC(15, 2) NOT NULL,
    applicable_from  TIME,
    applicable_to    TIME,
    machine_zone     VARCHAR(50),
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_price_plans_price_positive CHECK (price_per_hour > 0)
);

CREATE INDEX idx_price_plans_is_active    ON price_plans (is_active);
CREATE INDEX idx_price_plans_machine_zone ON price_plans (machine_zone);

-- ============================================================
-- SESSIONS
-- ============================================================
CREATE TABLE sessions (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT         NOT NULL REFERENCES users (id),
    machine_id               BIGINT         NOT NULL REFERENCES machines (id),
    started_at               TIMESTAMP      NOT NULL DEFAULT NOW(),
    ended_at                 TIMESTAMP,
    duration_seconds         BIGINT,
    total_cost               NUMERIC(15, 2),
    status                   VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    price_plan_id            BIGINT         REFERENCES price_plans (id),
    price_per_hour_snapshot  NUMERIC(15, 2) NOT NULL,
    created_at               TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_sessions_status CHECK (status IN ('ACTIVE', 'ENDED', 'FORCE_ENDED')),
    CONSTRAINT chk_sessions_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_sessions_user_id      ON sessions (user_id);
CREATE INDEX idx_sessions_machine_id   ON sessions (machine_id);
CREATE INDEX idx_sessions_status       ON sessions (status);
CREATE INDEX idx_sessions_user_status  ON sessions (user_id, status);

-- ============================================================
-- TRANSACTIONS
-- ============================================================
CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users (id),
    type            VARCHAR(20)    NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    balance_before  NUMERIC(15, 2) NOT NULL,
    balance_after   NUMERIC(15, 2) NOT NULL,
    description     VARCHAR(255),
    payment_method  VARCHAR(20),
    reference_code  VARCHAR(100)   UNIQUE,
    session_id      BIGINT         REFERENCES sessions (id),
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_transactions_type           CHECK (type IN ('TOPUP', 'DEDUCT', 'REFUND')),
    CONSTRAINT chk_transactions_payment_method CHECK (payment_method IN ('QR_BANK', 'CASH', 'ADMIN') OR payment_method IS NULL),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_id       ON transactions (user_id);
CREATE INDEX idx_transactions_session_id    ON transactions (session_id);
CREATE INDEX idx_transactions_type          ON transactions (type);
CREATE INDEX idx_transactions_created_at    ON transactions (created_at);
CREATE INDEX idx_transactions_reference_code ON transactions (reference_code);

-- ============================================================
-- QR PAYMENTS
-- ============================================================
CREATE TABLE qr_payments (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users (id),
    machine_id       BIGINT         REFERENCES machines (id),
    amount_expected  NUMERIC(15, 2) NOT NULL,
    amount_received  NUMERIC(15, 2),
    reference_code   VARCHAR(100)   NOT NULL UNIQUE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    expired_at       TIMESTAMP      NOT NULL,
    matched_at       TIMESTAMP,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_qr_payments_status CHECK (status IN ('PENDING', 'MATCHED', 'EXPIRED')),
    CONSTRAINT chk_qr_payments_amount_positive CHECK (amount_expected > 0)
);

CREATE INDEX idx_qr_payments_user_id        ON qr_payments (user_id);
CREATE INDEX idx_qr_payments_machine_id     ON qr_payments (machine_id);
CREATE INDEX idx_qr_payments_status         ON qr_payments (status);
CREATE INDEX idx_qr_payments_reference_code ON qr_payments (reference_code);

-- ============================================================
-- MENU ITEMS
-- ============================================================
CREATE TABLE menu_items (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL,
    price        NUMERIC(15, 2) NOT NULL,
    category     VARCHAR(50),
    image_url    TEXT,
    is_available BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_menu_items_price_positive CHECK (price > 0)
);

CREATE INDEX idx_menu_items_category     ON menu_items (category);
CREATE INDEX idx_menu_items_is_available ON menu_items (is_available);

-- ============================================================
-- ORDERS
-- ============================================================
CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT         NOT NULL REFERENCES sessions (id),
    user_id     BIGINT         NOT NULL REFERENCES users (id),
    machine_id  BIGINT         NOT NULL REFERENCES machines (id),
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    note        VARCHAR(500),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'DELIVERING', 'DONE'))
);

CREATE INDEX idx_orders_session_id ON orders (session_id);
CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_machine_id ON orders (machine_id);
CREATE INDEX idx_orders_status     ON orders (status);

-- ============================================================
-- ORDER ITEMS
-- ============================================================
CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders (id),
    item_id    BIGINT         NOT NULL REFERENCES menu_items (id),
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,

    CONSTRAINT chk_order_items_quantity_positive   CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price_positive CHECK (unit_price > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_item_id  ON order_items (item_id);

-- ============================================================
-- FEEDBACKS
-- ============================================================
CREATE TABLE feedbacks (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    machine_id BIGINT    REFERENCES machines (id),
    session_id BIGINT    REFERENCES sessions (id),
    content    VARCHAR(1000),
    rating     INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_feedbacks_rating CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5))
);

CREATE INDEX idx_feedbacks_user_id    ON feedbacks (user_id);
CREATE INDEX idx_feedbacks_machine_id ON feedbacks (machine_id);
CREATE INDEX idx_feedbacks_session_id ON feedbacks (session_id);
