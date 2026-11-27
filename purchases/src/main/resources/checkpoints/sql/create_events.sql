CREATE TABLE IF NOT EXISTS events (
    event_type TEXT NOT NULL,
    name TEXT NOT NULL,
    timestamp_ms INTEGER NOT NULL,
    properties TEXT
);
