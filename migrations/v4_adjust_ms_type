ALTER TABLE submissions
  ALTER COLUMN time_ms TYPE BIGINT,
  ALTER COLUMN time_ms SET DEFAULT 0,
  DROP CONSTRAINT submissions_time_ms_check,
  ADD CONSTRAINT submissions_time_ms_check CHECK (time_ms >= 0);
