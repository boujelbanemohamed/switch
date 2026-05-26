package com.switchplatform.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participants")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParticipantType type;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @Column(length = 512)
    private String endpointUrl;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private EndpointType endpointType;

    @Column(columnDefinition = "VARCHAR(50)[]")
    private String[] supportedProtocols;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ParticipantType {
        ACQUIRER, ISSUER, SWITCH, PROCESSOR
    }

    public enum ParticipantStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public enum EndpointType {
        TCP, HTTP, MQ, FILE
    }
}
