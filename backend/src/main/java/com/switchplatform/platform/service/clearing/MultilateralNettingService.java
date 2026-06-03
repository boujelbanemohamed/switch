package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.model.clearing.MultilateralNettingSession;
import com.switchplatform.platform.model.clearing.MultilateralPosition;
import com.switchplatform.platform.model.clearing.MultilateralPosition.PositionType;
import com.switchplatform.platform.repository.clearing.MultilateralNettingSessionRepository;
import com.switchplatform.platform.repository.clearing.MultilateralPositionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultilateralNettingService {

    private final MultilateralNettingSessionRepository sessionRepository;
    private final MultilateralPositionRepository positionRepository;
    private final EntityManager entityManager;

    @Transactional
    public MultilateralNettingSession calculateNetting(LocalDate sessionDate, String currencyCode) {
        MultilateralNettingSession session = MultilateralNettingSession.builder()
                .sessionDate(sessionDate)
                .status(MultilateralNettingSession.Status.CALCULATING)
                .currencyCode(currencyCode)
                .createdAt(OffsetDateTime.now())
                .build();
        session = sessionRepository.save(session);

        List<Tuple> rows = entityManager.createQuery(
                "SELECT cr.acquiringParticipantId AS pid, " +
                "       SUM(cr.amount) AS total " +
                "FROM ClearingRecord cr " +
                "WHERE cr.clearingDate = :date AND cr.currencyCode = :ccy " +
                "AND cr.status = 'CLEARED' " +
                "GROUP BY cr.acquiringParticipantId", Tuple.class)
                .setParameter("date", sessionDate)
                .setParameter("ccy", currencyCode)
                .getResultList();

        Map<UUID, BigDecimal> credits = new HashMap<>();
        Map<UUID, BigDecimal> debits = new HashMap<>();

        for (Tuple row : rows) {
            UUID pid = row.get("pid", UUID.class);
            BigDecimal total = row.get("total", BigDecimal.class);
            credits.merge(pid, total, BigDecimal::add);
        }

        rows = entityManager.createQuery(
                "SELECT cr.issuingParticipantId AS pid, " +
                "       SUM(cr.amount) AS total " +
                "FROM ClearingRecord cr " +
                "WHERE cr.clearingDate = :date AND cr.currencyCode = :ccy " +
                "AND cr.status = 'CLEARED' " +
                "GROUP BY cr.issuingParticipantId", Tuple.class)
                .setParameter("date", sessionDate)
                .setParameter("ccy", currencyCode)
                .getResultList();

        for (Tuple row : rows) {
            UUID pid = row.get("pid", UUID.class);
            BigDecimal total = row.get("total", BigDecimal.class);
            debits.merge(pid, total, BigDecimal::add);
        }

        Set<UUID> allParticipants = new HashSet<>();
        allParticipants.addAll(credits.keySet());
        allParticipants.addAll(debits.keySet());

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (UUID pid : allParticipants) {
            BigDecimal credit = credits.getOrDefault(pid, BigDecimal.ZERO);
            BigDecimal debit = debits.getOrDefault(pid, BigDecimal.ZERO);
            BigDecimal net = credit.subtract(debit);
            PositionType pt = net.compareTo(BigDecimal.ZERO) > 0 ? PositionType.CREDIT
                    : net.compareTo(BigDecimal.ZERO) < 0 ? PositionType.DEBIT
                    : PositionType.NEUTRAL;

            MultilateralPosition pos = MultilateralPosition.builder()
                    .sessionId(session.getId())
                    .participantId(pid)
                    .grossDebit(debit)
                    .grossCredit(credit)
                    .netPosition(net.abs())
                    .positionType(pt)
                    .settlementStatus("PENDING")
                    .build();
            positionRepository.save(pos);

            totalGross = totalGross.add(credit).add(debit);
            totalNet = totalNet.add(net.abs());
        }

        BigDecimal efficiency = totalGross.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ONE.subtract(
                        totalNet.divide(totalGross, 10, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        session.setStatus(MultilateralNettingSession.Status.CALCULATED);
        session.setTotalGrossAmount(totalGross);
        session.setTotalNetAmount(totalNet);
        session.setNettingEfficiency(efficiency);
        session = sessionRepository.save(session);

        log.info("Netting calculated: session={} efficiency={}% participants={}",
                session.getId(), efficiency, allParticipants.size());
        return session;
    }

    @Transactional
    public MultilateralNettingSession confirmSession(UUID sessionId) {
        MultilateralNettingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        s.setStatus(MultilateralNettingSession.Status.CONFIRMED);
        return sessionRepository.save(s);
    }

    @Transactional
    public MultilateralNettingSession settleSession(UUID sessionId) {
        MultilateralNettingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        List<MultilateralPosition> positions = positionRepository.findBySessionId(sessionId);
        for (MultilateralPosition p : positions) {
            p.setSettlementStatus("SETTLED");
            p.setSettledAt(OffsetDateTime.now());
            p.setSettlementReference("STL-" + UUID.randomUUID().toString().substring(0, 8));
            positionRepository.save(p);
        }
        s.setStatus(MultilateralNettingSession.Status.SETTLED);
        return sessionRepository.save(s);
    }

    public List<MultilateralPosition> getPositions(UUID sessionId) {
        return positionRepository.findBySessionId(sessionId);
    }

    public Optional<MultilateralNettingSession> getLatestSession() {
        return sessionRepository.findTopByStatusOrderByCreatedAtDesc(MultilateralNettingSession.Status.CALCULATED);
    }
}
