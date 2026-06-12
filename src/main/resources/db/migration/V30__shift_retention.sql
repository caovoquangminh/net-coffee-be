-- Giữ lại tối đa 60 ngày ca làm gần nhất. Cho phép xóa ca cũ kéo theo dữ liệu phụ thuộc.
-- attendance_records / overtime_requests tham chiếu work_shifts nhưng chưa có ON DELETE CASCADE.

ALTER TABLE attendance_records DROP CONSTRAINT IF EXISTS attendance_records_shift_id_fkey;
ALTER TABLE attendance_records
    ADD CONSTRAINT attendance_records_shift_id_fkey
    FOREIGN KEY (shift_id) REFERENCES work_shifts (id) ON DELETE CASCADE;

ALTER TABLE overtime_requests DROP CONSTRAINT IF EXISTS overtime_requests_shift_id_fkey;
ALTER TABLE overtime_requests
    ADD CONSTRAINT overtime_requests_shift_id_fkey
    FOREIGN KEY (shift_id) REFERENCES work_shifts (id) ON DELETE CASCADE;

-- Dọn ngay các ca cũ hơn 60 ngày (dữ liệu phụ thuộc tự xóa theo CASCADE;
-- lương tháng đã chốt nằm ở payroll_records nên không bị ảnh hưởng).
DELETE FROM work_shifts WHERE shift_date < (CURRENT_DATE - INTERVAL '60 days');
