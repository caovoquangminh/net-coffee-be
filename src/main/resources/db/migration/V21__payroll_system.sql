CREATE TABLE payroll_periods (
    id           BIGSERIAL PRIMARY KEY,
    year         SMALLINT    NOT NULL,
    month        SMALLINT    NOT NULL CHECK (month BETWEEN 1 AND 12),
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SENT','CONFIRMED')),
    sent_at      TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (year, month)
);

CREATE TABLE payroll_records (
    id             BIGSERIAL PRIMARY KEY,
    period_id      BIGINT         NOT NULL REFERENCES payroll_periods(id),
    user_id        BIGINT         NOT NULL REFERENCES users(id),
    total_hours    NUMERIC(8,2)   NOT NULL DEFAULT 0,
    hourly_wage    NUMERIC(12,2)  NOT NULL DEFAULT 0,
    base_salary    NUMERIC(15,2)  NOT NULL DEFAULT 0,
    overtime_hours NUMERIC(8,2)   NOT NULL DEFAULT 0,
    overtime_pay   NUMERIC(15,2)  NOT NULL DEFAULT 0,
    bonus          NUMERIC(15,2)  NOT NULL DEFAULT 0,
    penalty        NUMERIC(15,2)  NOT NULL DEFAULT 0,
    responsibility NUMERIC(15,2)  NOT NULL DEFAULT 0,
    advance        NUMERIC(15,2)  NOT NULL DEFAULT 0,
    total_salary   NUMERIC(15,2)  NOT NULL DEFAULT 0,
    pay_status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING' CHECK (pay_status IN ('PENDING','CONFIRMED','DISPUTED')),
    dispute_reason VARCHAR(500),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE (period_id, user_id)
);
CREATE INDEX idx_payroll_rec_period ON payroll_records (period_id);
CREATE INDEX idx_payroll_rec_user   ON payroll_records (user_id);
