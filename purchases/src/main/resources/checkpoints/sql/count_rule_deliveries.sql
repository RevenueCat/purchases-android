SELECT COUNT(*) FROM events
WHERE event_type = 'rule_delivered' AND name = ? AND timestamp_ms BETWEEN ? AND ?;
