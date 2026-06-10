package com.switchplatform.platform.model.fraud;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "device_fingerprints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFingerprintRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(length = 50)
    private String os;

    @Column(length = 50)
    private String browser;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 3)
    private String country;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> attributes;

    @Column(name = "usage_count")
    private int usageCount;

    @Column(name = "first_seen")
    private OffsetDateTime firstSeen;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;
}
