-- ============================================================================
-- Attendance / Shift / OT rework
-- Quán net thật: ca 06:00-11:00 / 11:00-17:00 / 17:00-22:00, NV tự đăng ký ca
-- tuần sau (cửa sổ cuối tuần), chấm công +-15p kèm lý do, auto-checkout, OT/đổi
-- ca/nghỉ phép duyệt qua Telegram, tính công làm tròn theo ca, redistribution.
-- ============================================================================

-- 1) Dọn ca tương lai dùng giờ cũ (07/15/23h) để scheduler tạo lại theo giờ mới.
--    Chỉ xóa ca chưa phát sinh chấm công, từ hôm nay trở đi. Registration cascade.
DELETE FROM work_shifts ws
 WHERE ws.shift_date >= CURRENT_DATE
   AND NOT EXISTS (SELECT 1 FROM attendance_records ar WHERE ar.shift_id = ws.id);

-- 2) shift_registrations: bỏ bước duyệt (APPROVED). Trạng thái mới.
UPDATE shift_registrations SET status = 'REGISTERED' WHERE status = 'APPROVED';
ALTER TABLE shift_registrations DROP CONSTRAINT IF EXISTS shift_registrations_status_check;
ALTER TABLE shift_registrations
    ADD CONSTRAINT shift_registrations_status_check
    CHECK (status IN ('REGISTERED','CANCELLED','ADMIN_ASSIGNED','COMPLETED','ABSENT'));

-- 3) attendance_records: thêm cột tính công chi tiết + trạng thái AUTO_CHECKOUT.
ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS late_minutes         INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS early_leave_minutes  INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS redistributed_minutes INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS checkout_reason      VARCHAR(500);

ALTER TABLE attendance_records DROP CONSTRAINT IF EXISTS attendance_records_attend_status_check;
ALTER TABLE attendance_records
    ADD CONSTRAINT attendance_records_attend_status_check
    CHECK (attend_status IN ('ON_TIME','LATE','EARLY_LEAVE','ABSENT','AUTO_CHECKOUT'));

-- 4) overtime_requests: giờ OT cụ thể + người thay thế.
ALTER TABLE overtime_requests
    ADD COLUMN IF NOT EXISTS ot_start_time       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ot_end_time         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS replacement_user_id BIGINT REFERENCES users(id);

-- 5) Đổi ca.
CREATE TABLE shift_swap_requests (
    id                  BIGSERIAL PRIMARY KEY,
    from_user_id        BIGINT      NOT NULL REFERENCES users(id),
    to_user_id          BIGINT      NOT NULL REFERENCES users(id),
    shift_id            BIGINT      NOT NULL REFERENCES work_shifts(id) ON DELETE CASCADE,
    reason              VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    telegram_message_id VARCHAR(100),
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_swap_from_user ON shift_swap_requests (from_user_id);
CREATE INDEX idx_swap_to_user   ON shift_swap_requests (to_user_id);
CREATE INDEX idx_swap_shift      ON shift_swap_requests (shift_id);

-- 6) Nghỉ phép.
CREATE TABLE leave_requests (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users(id),
    shift_id            BIGINT      REFERENCES work_shifts(id) ON DELETE SET NULL,
    leave_date          DATE        NOT NULL,
    leave_type          VARCHAR(20) NOT NULL
                        CHECK (leave_type IN ('ANNUAL','SICK','EMERGENCY')),
    reason              VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    telegram_message_id VARCHAR(100),
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_leave_user ON leave_requests (user_id);
CREATE INDEX idx_leave_date ON leave_requests (leave_date);

-- 7) Cấu hình hệ thống (key-value): Telegram group chat + tham số thưởng/phạt.
CREATE TABLE app_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(500),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('telegram_chat_id',          NULL),
    ('telegram_bot_token',        NULL),
    ('attendance_bonus',          '200000'),
    ('late_penalty_per_minute',   '1000'),
    ('absent_penalty',            '200000'),
    ('overtime_multiplier',       '1.5')
ON CONFLICT (setting_key) DO NOTHING;
