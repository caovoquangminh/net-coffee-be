ALTER TABLE transactions ADD COLUMN performed_by_user_id BIGINT REFERENCES users(id);

COMMENT ON COLUMN transactions.performed_by_user_id IS 'Nhân viên/admin thực hiện giao dịch (NULL = tự động)';

CREATE INDEX idx_transactions_performed_by ON transactions(performed_by_user_id);
