package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.NettingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NettingRecordRepository extends JpaRepository<NettingRecord, UUID> {
    List<NettingRecord> findByNettingDate(LocalDate date);
    List<NettingRecord> findByParticipantId(UUID participantId);
}
