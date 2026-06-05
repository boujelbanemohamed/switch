package com.switchplatform.platform.repository.credit;

import com.switchplatform.platform.model.credit.InstallmentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentEntryRepository extends JpaRepository<InstallmentEntry, UUID> {
    List<InstallmentEntry> findByInstallmentPlanIdOrderBySequenceNumberAsc(UUID installmentPlanId);
    List<InstallmentEntry> findByInstallmentPlanIdAndStatementId(UUID installmentPlanId, UUID statementId);
    List<InstallmentEntry> findByStatementId(UUID statementId);
}
