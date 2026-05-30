CREATE TABLE mq_dlq (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_retry_at TIMESTAMP WITH TIME ZONE
);
