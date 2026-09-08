ALTER TABLE relief_requests ADD COLUMN verification_round INTEGER NOT NULL DEFAULT 1 CHECK (verification_round >= 1);
ALTER TABLE verification_records ADD COLUMN verification_round INTEGER NOT NULL DEFAULT 1 CHECK (verification_round >= 1);
CREATE INDEX IF NOT EXISTS idx_active_request_duplicate ON relief_requests(disaster_event_id, affected_area_id, state);
CREATE INDEX IF NOT EXISTS idx_verification_request_round ON verification_records(request_id, verification_round, id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_verification_one_level_per_round ON verification_records(request_id, verification_round, level);
