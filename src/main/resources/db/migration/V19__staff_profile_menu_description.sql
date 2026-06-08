-- Mở rộng bảng users với thông tin hồ sơ nhân viên (tất cả nullable, backward-compatible)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS staff_address  VARCHAR(300),
    ADD COLUMN IF NOT EXISTS id_card        VARCHAR(30),
    ADD COLUMN IF NOT EXISTS staff_email    VARCHAR(150),
    ADD COLUMN IF NOT EXISTS birth_date     DATE,
    ADD COLUMN IF NOT EXISTS start_date     DATE,
    ADD COLUMN IF NOT EXISTS hourly_wage    NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS deleted_at     TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);

-- Thêm mô tả cho menu items và hỗ trợ image upload
ALTER TABLE menu_items
    ADD COLUMN IF NOT EXISTS description VARCHAR(500);
