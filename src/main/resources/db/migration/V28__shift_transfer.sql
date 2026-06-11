-- ============================================================================
-- Làm thay một phần ca (shift transfer / partial coverage)
-- A nhờ B làm thay một đoạn của ca → admin duyệt → ca được CHIA thành các đoạn
-- (shift_assignments). Check-in/out & tính công dựa trên ĐOẠN của mỗi người, không
-- phải giờ ca gốc.
-- ============================================================================

CREATE TABLE shift_transfer_requests (
    id                   BIGSERIAL PRIMARY KEY,
    shift_id             BIGINT      NOT NULL REFERENCES work_shifts(id) ON DELETE CASCADE,
    original_user_id     BIGINT      NOT NULL REFERENCES users(id),   -- A (người nhờ)
    replacement_user_id  BIGINT      NOT NULL REFERENCES users(id),   -- B (người làm thay)
    start_time           TIMESTAMP   NOT NULL,                        -- đoạn B làm thay
    end_time             TIMESTAMP   NOT NULL,
    reason               VARCHAR(500),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    approved_by          BIGINT      REFERENCES users(id),
    approved_at          TIMESTAMP,
    telegram_message_id  VARCHAR(100),
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_transfer_shift     ON shift_transfer_requests (shift_id);
CREATE INDEX idx_transfer_original  ON shift_transfer_requests (original_user_id);
CREATE INDEX idx_transfer_replace   ON shift_transfer_requests (replacement_user_id);

-- Đoạn ca thực tế của từng nhân viên (sau khi chia). Mỗi (user, shift) tối đa 1 đoạn.
CREATE TABLE shift_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    shift_id            BIGINT      NOT NULL REFERENCES work_shifts(id) ON DELETE CASCADE,
    user_id             BIGINT      NOT NULL REFERENCES users(id),
    start_time          TIMESTAMP   NOT NULL,
    end_time            TIMESTAMP   NOT NULL,
    source              VARCHAR(20) NOT NULL DEFAULT 'REGISTRATION'
                        CHECK (source IN ('REGISTRATION','TRANSFER','ADMIN')),
    transfer_request_id BIGINT      REFERENCES shift_transfer_requests(id) ON DELETE SET NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (shift_id, user_id)
);
CREATE INDEX idx_assignment_shift ON shift_assignments (shift_id);
CREATE INDEX idx_assignment_user  ON shift_assignments (user_id);
