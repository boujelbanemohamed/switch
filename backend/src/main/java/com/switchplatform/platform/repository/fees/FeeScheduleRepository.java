package com.switchplatform.platform.repository.fees;

import com.switchplatform.platform.model.fees.FeeSchedule;
import com.switchplatform.platform.model.fees.FeeSchedule.ScheduleType;
import com.switchplatform.platform.model.fees.FeeSchedule.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, UUID> {

    List<FeeSchedule> findByScheduleType(ScheduleType scheduleType);

    List<FeeSchedule> findByStatus(Status status);

    List<FeeSchedule> findByScheduleTypeAndStatus(ScheduleType scheduleType, Status status);

    List<FeeSchedule> findByParticipantId(UUID participantId);

    List<FeeSchedule> findByMerchantId(UUID merchantId);

    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.status = 'ACTIVE' " +
           "AND fs.effectiveFrom <= :date AND (fs.effectiveUntil IS NULL OR fs.effectiveUntil >= :date) " +
           "ORDER BY fs.priority DESC")
    List<FeeSchedule> findActiveSchedules(LocalDate date);
}
