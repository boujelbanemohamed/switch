package com.switchplatform.platform.service.simulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Slf4j
public class SimulatedDirectoryServer {

    public DsRoutingResult routeForCard(UUID cardId, String transactionId, UUID authId, UUID sessionId, UUID merchantId) {
        log.info("Directory Server routing AReq: cardId={}, authId={}, sessionId={}, merchantId={}",
                cardId, authId, sessionId, merchantId);

        return DsRoutingResult.builder()
                .acsReferenceNumber("ACS-SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .acsUrl("https://acs.switchplatform.com/3ds")
                .threeDsServerUrl("https://ds.switchplatform.com/3ds")
                .dsReferenceNumber("DS-SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .dsTransId(UUID.randomUUID().toString())
                .routedAt(OffsetDateTime.now())
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DsRoutingResult {
        private String acsReferenceNumber;
        private String acsUrl;
        private String threeDsServerUrl;
        private String dsReferenceNumber;
        private String dsTransId;
        private OffsetDateTime routedAt;
    }
}
