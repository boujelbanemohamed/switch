-- Notification preferences for per-user channel/event type opt-in
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    contact_value VARCHAR(255),
    event_types TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_prefs_user_id ON notification_preferences(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_notif_prefs_user_channel ON notification_preferences(user_id, channel);
