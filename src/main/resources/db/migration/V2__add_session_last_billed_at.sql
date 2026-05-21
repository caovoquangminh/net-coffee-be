-- Track billing precision: stores the timestamp up to which the session has been billed.
-- Set to (started_at + minimum_minutes) after chargeMinimumFee, then updated each billing tick.
-- Allows final settlement on session end to charge for the exact seconds used.
ALTER TABLE sessions ADD COLUMN last_billed_at TIMESTAMP;
