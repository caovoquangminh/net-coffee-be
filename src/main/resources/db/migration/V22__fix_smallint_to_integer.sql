-- Flyway: convert SMALLINT columns to INTEGER to match Hibernate entity mapping
ALTER TABLE work_shifts
    ALTER COLUMN shift_number TYPE INTEGER;

ALTER TABLE payroll_periods
    ALTER COLUMN year  TYPE INTEGER,
    ALTER COLUMN month TYPE INTEGER;
