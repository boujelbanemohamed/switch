package com.switchplatform.platform.model.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "contact_value", length = 255)
    private String contactValue;

    @Column(name = "event_types", columnDefinition = "TEXT")
    private String eventTypes;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
