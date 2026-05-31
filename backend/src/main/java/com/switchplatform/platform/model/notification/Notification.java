package com.switchplatform.platform.model.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 64)
    private String type;

    @Column(length = 64)
    private String recipient;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 20)
    private String channel;

    @Column(length = 20)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
