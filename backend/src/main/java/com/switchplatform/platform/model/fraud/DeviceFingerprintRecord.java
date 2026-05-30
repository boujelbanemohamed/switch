package com.switchplatform.platform.model.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFingerprintRecord {
    private UUID id;
    private String cardId;
    private String deviceId;
    private String deviceType;
    private String os;
    private String browser;
    private String userAgent;
    private String ipAddress;
    private String country;
    private Map<String, String> attributes;
    private int usageCount;
    private OffsetDateTime firstSeen;
    private OffsetDateTime lastSeen;
}
