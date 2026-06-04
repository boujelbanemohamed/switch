package com.switchplatform.platform.service.network;

import com.solab.iso8583.IsoMessage;
import com.switchplatform.platform.iso8583.Iso8583Engine;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkManagementService {

    private final ParticipantRepository participantRepository;
    private final Iso8583Engine iso8583Engine;

    @Scheduled(fixedRateString = "${switch.network.heartbeat.interval:30000}")
    public void sendHeartbeatToAllParticipants() {
        List<Participant> activeParticipants = participantRepository.findByTypeAndStatus(
                Participant.ParticipantType.ISSUER, Participant.ParticipantStatus.ACTIVE);
        activeParticipants.addAll(participantRepository.findByTypeAndStatus(
                Participant.ParticipantType.ACQUIRER, Participant.ParticipantStatus.ACTIVE));

        for (Participant participant : activeParticipants) {
            if (participant.getEndpointUrl() == null || participant.getEndpointUrl().isBlank()) {
                continue;
            }
            try {
                IsoMessage echoRequest = iso8583Engine.createNetworkManagementRequest("301");
                byte[] requestData = iso8583Engine.encode(echoRequest);
                log.debug("Sending echo test to participant={} url={}", participant.getCode(), participant.getEndpointUrl());
            } catch (Exception e) {
                log.warn("Failed to send heartbeat to participant {}: {}", participant.getCode(), e.getMessage());
            }
        }
    }
}
