package com.switchplatform.platform.service.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceScoreResult {
    private double score;
    private boolean isKnownDevice;
    private boolean isSuspiciousUserAgent;
    private boolean isNewCountry;
    private String recommendation;
    private List<String> reasons;
}
