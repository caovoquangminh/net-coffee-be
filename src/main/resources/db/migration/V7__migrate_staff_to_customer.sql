-- Chuyển tất cả tài khoản STAFF hiện có thành CUSTOMER (tài khoản hội viên).
-- Tài khoản nhân viên thực sự sẽ được admin tạo lại hoặc cập nhật role thủ công.
UPDATE users SET role = 'CUSTOMER' WHERE role = 'STAFF';
