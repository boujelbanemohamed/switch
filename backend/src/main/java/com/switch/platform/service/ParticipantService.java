package com.switch.platform.service;

import com.switch.platform.model.Participant;
import com.switch.platform.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public Participant findByCode(String code) {
        return participantRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Participant not found: " + code));
    }

    public Participant findById(UUID id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Participant not found: " + id));
    }

    public List<Participant> findAll() {
        return participantRepository.findAll();
    }

    public Participant create(Participant participant) {
        if (participantRepository.existsByCode(participant.getCode())) {
            throw new IllegalArgumentException(
                    "Participant code already exists: " + participant.getCode());
        }
        return participantRepository.save(participant);
    }

    public Participant update(UUID id, Participant participant) {
        Participant existing = findById(id);
        existing.setName(participant.getName());
        existing.setStatus(participant.getStatus());
        existing.setEndpointUrl(participant.getEndpointUrl());
        existing.setEndpointType(participant.getEndpointType());
        existing.setMetadata(participant.getMetadata());
        return participantRepository.save(existing);
    }

    public void delete(UUID id) {
        participantRepository.deleteById(id);
    }
}
