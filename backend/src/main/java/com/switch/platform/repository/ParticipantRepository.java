package com.switch.platform.repository;

import com.switch.platform.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByCode(String code);
    List<Participant> findByStatus(Participant.ParticipantStatus status);
    List<Participant> findByType(Participant.ParticipantType type);
    boolean existsByCode(String code);
}
