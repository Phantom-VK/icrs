ALTER TABLE IF EXISTS grievances
    ADD COLUMN IF NOT EXISTS priority VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sentiment VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ai_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ai_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_resolution_text TEXT,
    ADD COLUMN IF NOT EXISTS ai_resolution_comment TEXT,
    ADD COLUMN IF NOT EXISTS ai_model_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_decision_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ai_decision_source VARCHAR(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'grievances'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_grievances_ai_resolved ON grievances (ai_resolved);
        CREATE INDEX IF NOT EXISTS idx_grievances_priority ON grievances (priority);
        CREATE INDEX IF NOT EXISTS idx_grievances_sentiment ON grievances (sentiment);
    END IF;
END $$;
