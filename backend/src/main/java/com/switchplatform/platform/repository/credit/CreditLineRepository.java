package com.switchplatform.platform.repository.credit;

import com.switchplatform.platform.model.credit.CreditLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditLineRepository extends JpaRepository<CreditLine, UUID> {
    Optional<CreditLine> findByCardAccountId(UUID cardAccountId);
    List<CreditLine> findByStatus(CreditLine.CreditLineStatus status);
}
