-- Mở rộng check constraint để hỗ trợ role CUSTOMER (tài khoản hội viên)
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'STAFF', 'CUSTOMER'));
