CREATE INDEX IF NOT EXISTS events_by_type_name_and_time
ON events (event_type, name, timestamp_ms);
