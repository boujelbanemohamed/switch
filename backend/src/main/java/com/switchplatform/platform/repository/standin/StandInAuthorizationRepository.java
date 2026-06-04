package com.switchplatform.platform.repository.standin;

import com.switchplatform.platform.model.standin.StandInAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StandInAuthorizationRepository extends JpaRepository<StandInAuthorization, UUID> {

    List<StandInAuthorization> findByReconciledFalse();

    List<StandInAuthorization> findByIssuerParticipantIdAndReconciledFalse(UUID issuerParticipantId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM StandInAuthorization s " +
           "WHERE s.issuerParticipantId = :issuerId " +
           "AND s.decision = 'APPROVED' " +
           "AND CAST(s.authorizedAt AS date) = :date")
    BigDecimal sumApprovedAmountForIssuerOnDate(@Param("issuerId") UUID issuerId,
                                                  @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM StandInAuthorization s " +
           "WHERE s.issuerParticipantId = :issuerId " +
           "AND s.decision = 'APPROVED' " +
           "AND CAST(s.authorizedAt AS date) = :date")
    long countApprovedForIssuerOnDate(@Param("issuerId") UUID issuerId,
                                       @Param("date") LocalDate date);
}
