package com.switchplatform.platform.repository.credit;

import com.switchplatform.platform.model.credit.CreditStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditStatementRepository extends JpaRepository<CreditStatement, UUID> {
    List<CreditStatement> findByCreditLineIdOrderByStatementDateDesc(UUID creditLineId);
    Optional<CreditStatement> findTopByCreditLineIdOrderByStatementDateDesc(UUID creditLineId);
    List<CreditStatement> findByCreditLineIdAndStatementDate(UUID creditLineId, LocalDate statementDate);
    List<CreditStatement> findByStatus(CreditStatement.StatementStatus status);
}
