package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.RecurringSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, UUID> {
    List<RecurringSchedule> findByCofTokenId(UUID cofTokenId);
    List<RecurringSchedule> findByNextRunDateLessThanEqualAndStatus(LocalDate date, String status);
}
