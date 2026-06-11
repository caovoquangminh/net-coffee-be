-- Lưu lịch sử tin nhắn giao tiếp khách ↔ nhân viên.
-- Trước đây tin chỉ broadcast qua STOMP nên mất sạch khi đóng/mở lại cửa sổ chat
-- hoặc khi gửi lúc người nhận chưa mở chat (chỉ kịp hiện notification).
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    user_id BIGINT,
    sender VARCHAR(20) NOT NULL, -- 'CUSTOMER' | 'STAFF'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_machine_created
    ON chat_messages (machine_id, created_at);

CREATE INDEX IF NOT EXISTS idx_chat_messages_machine_user_created
    ON chat_messages (machine_id, user_id, created_at);
