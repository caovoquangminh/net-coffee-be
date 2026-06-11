-- OT đúng nghĩa: phần làm THÊM sau khi hết ca của chính nhân viên (vd ca 06:00-11:00,
-- làm tới 13:00 → 11:00-13:00 là OT). Lưu riêng giờ OT để cộng lương (×hệ số).
ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS ot_hours NUMERIC(5,2) NOT NULL DEFAULT 0;
