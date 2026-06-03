package com.switchplatform.platform.service.dispute;

import com.switchplatform.platform.event.DisputeOpenedEvent;
import com.switchplatform.platform.event.DisputeResolvedEvent;
import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.model.dispute.Dispute;
import com.switchplatform.platform.model.dispute.DisputeEvidence;
import com.switchplatform.platform.model.dispute.DisputeTimeline;
import com.switchplatform.platform.repository.dispute.DisputeEvidenceRepository;
import com.switchplatform.platform.repository.dispute.DisputeRepository;
import com.switchplatform.platform.repository.dispute.DisputeTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final DisputeTimelineRepository timelineRepository;
    private final EventPublisher eventPublisher;

    private static final Map<Dispute.DisputeStatus, Set<Dispute.DisputeStatus>> VALID_TRANSITIONS = Map.of(
            Dispute.DisputeStatus.OPEN, Set.of(Dispute.DisputeStatus.UNDER_REVIEW, Dispute.DisputeStatus.WITHDRAWN),
            Dispute.DisputeStatus.UNDER_REVIEW, Set.of(Dispute.DisputeStatus.EVIDENCE_REQUESTED, Dispute.DisputeStatus.WON, Dispute.DisputeStatus.LOST),
            Dispute.DisputeStatus.EVIDENCE_REQUESTED, Set.of(Dispute.DisputeStatus.EVIDENCE_SUBMITTED),
            Dispute.DisputeStatus.EVIDENCE_SUBMITTED, Set.of(Dispute.DisputeStatus.REPRESENTMENT, Dispute.DisputeStatus.WON, Dispute.DisputeStatus.LOST),
            Dispute.DisputeStatus.REPRESENTMENT, Set.of(Dispute.DisputeStatus.PRE_ARBITRATION, Dispute.DisputeStatus.WON, Dispute.DisputeStatus.LOST),
            Dispute.DisputeStatus.PRE_ARBITRATION, Set.of(Dispute.DisputeStatus.ARBITRATION, Dispute.DisputeStatus.WON, Dispute.DisputeStatus.LOST),
            Dispute.DisputeStatus.ARBITRATION, Set.of(Dispute.DisputeStatus.WON, Dispute.DisputeStatus.LOST)
    );

    @Transactional
    public Dispute openDispute(String transactionId, Dispute.DisputeType type, BigDecimal amount,
                                String currency, String reasonCode, String reasonDescription,
                                String initiatedBy, UUID merchantId, UUID clearingRecordId) {
        String disputeNumber = "DSP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime evidenceDeadline = calculateEvidenceDeadline(type);

        Dispute dispute = Dispute.builder()
                .disputeNumber(disputeNumber)
                .transactionId(transactionId)
                .merchantId(merchantId)
                .clearingRecordId(clearingRecordId)
                .amount(amount)
                .currencyCode(currency)
                .disputeType(type)
                .status(Dispute.DisputeStatus.OPEN)
                .reasonCode(reasonCode)
                .reasonDescription(reasonDescription)
                .evidenceDeadline(evidenceDeadline)
                .resolutionDeadline(evidenceDeadline.plusDays(30))
                .initiatedBy(Dispute.InitiatedBy.valueOf(initiatedBy.toUpperCase()))
                .initiatedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        dispute = disputeRepository.save(dispute);
        log.info("Dispute opened: number={}, type={}, amount={} {}", disputeNumber, type, amount, currency);

        addTimelineEntry(dispute.getId(), "DISPUTE_OPENED", null, Dispute.DisputeStatus.OPEN.name(), "system", reasonDescription);

        eventPublisher.publishDisputeOpened(new DisputeOpenedEvent(
                dispute.getId(), disputeNumber, transactionId, type.name(),
                amount.toPlainString(), currency, initiatedBy, OffsetDateTime.now()));

        return dispute;
    }

    @Transactional
    public Dispute transitionStatus(UUID disputeId, Dispute.DisputeStatus newStatus, String performedBy, String notes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        Set<Dispute.DisputeStatus> allowed = VALID_TRANSITIONS.get(dispute.getStatus());
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new IllegalStateException("Invalid transition from " + dispute.getStatus() + " to " + newStatus);
        }

        Dispute.DisputeStatus oldStatus = dispute.getStatus();
        dispute.setStatus(newStatus);
        dispute.setUpdatedAt(OffsetDateTime.now());

        if (newStatus == Dispute.DisputeStatus.WON || newStatus == Dispute.DisputeStatus.LOST || newStatus == Dispute.DisputeStatus.WITHDRAWN) {
            dispute.setResolvedAt(OffsetDateTime.now());
            eventPublisher.publishDisputeResolved(new DisputeResolvedEvent(
                    disputeId, dispute.getDisputeNumber(), newStatus.name(), oldStatus.name(), OffsetDateTime.now()));
        }

        dispute = disputeRepository.save(dispute);
        log.info("Dispute {} transitioned: {} -> {} by {}", disputeId, oldStatus, newStatus, performedBy);

        addTimelineEntry(disputeId, "STATUS_CHANGED", oldStatus.name(), newStatus.name(), performedBy, notes);

        return dispute;
    }

    @Transactional
    public DisputeEvidence submitEvidence(UUID disputeId, String submittedBy,
                                           DisputeEvidence.EvidenceType type, String description, String fileReference) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        if (dispute.getEvidenceDeadline() != null && OffsetDateTime.now().isAfter(dispute.getEvidenceDeadline())) {
            throw new IllegalStateException("Evidence deadline has passed for dispute " + disputeId);
        }

        DisputeEvidence evidence = DisputeEvidence.builder()
                .disputeId(disputeId)
                .submittedBy(submittedBy)
                .evidenceType(type)
                .description(description)
                .fileReference(fileReference)
                .submittedAt(OffsetDateTime.now())
                .build();

        evidence = evidenceRepository.save(evidence);
        log.info("Evidence submitted for dispute {}: type={}, by={}", disputeId, type, submittedBy);

        addTimelineEntry(disputeId, "EVIDENCE_SUBMITTED", dispute.getStatus().name(), dispute.getStatus().name(), submittedBy,
                "Evidence " + type + " submitted");

        return evidence;
    }

    @Transactional(readOnly = true)
    public Dispute getDisputeWithTimeline(UUID disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));
    }

    public List<DisputeTimeline> getTimeline(UUID disputeId) {
        return timelineRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId);
    }

    public List<DisputeEvidence> getEvidence(UUID disputeId) {
        return evidenceRepository.findByDisputeIdOrderBySubmittedAtDesc(disputeId);
    }

    public List<Dispute> getOpenDisputes() {
        return disputeRepository.findByStatusIn(List.of(
                Dispute.DisputeStatus.OPEN,
                Dispute.DisputeStatus.UNDER_REVIEW,
                Dispute.DisputeStatus.EVIDENCE_REQUESTED,
                Dispute.DisputeStatus.EVIDENCE_SUBMITTED,
                Dispute.DisputeStatus.REPRESENTMENT,
                Dispute.DisputeStatus.PRE_ARBITRATION,
                Dispute.DisputeStatus.ARBITRATION
        ));
    }

    public List<Dispute> getDisputesByMerchant(UUID merchantId) {
        return disputeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<Dispute> getDisputesByTransaction(String transactionId) {
        return disputeRepository.findByTransactionId(transactionId);
    }

    public List<Dispute> listAll() {
        return disputeRepository.findAll();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiredDeadlines() {
        List<Dispute> expired = disputeRepository.findByEvidenceDeadlineBefore(OffsetDateTime.now());
        for (Dispute dispute : expired) {
            if (dispute.getStatus() == Dispute.DisputeStatus.EVIDENCE_REQUESTED) {
                log.warn("Evidence deadline passed for dispute {}, auto-escalating", dispute.getId());
                transitionStatus(dispute.getId(), Dispute.DisputeStatus.LOST, "SYSTEM",
                        "Evidence deadline expired. Dispute automatically lost.");
            }
        }
    }

    private OffsetDateTime calculateEvidenceDeadline(Dispute.DisputeType type) {
        int days = switch (type) {
            case FRAUD -> 30;
            case NOT_RECEIVED -> 45;
            case DUPLICATE -> 30;
            case INCORRECT_AMOUNT -> 45;
            case QUALITY_ISSUE -> 30;
            case CANCELLED -> 30;
            case CREDIT_NOT_PROCESSED -> 45;
            case OTHER -> 30;
        };
        return OffsetDateTime.now().plusDays(days);
    }

    private void addTimelineEntry(UUID disputeId, String action, String oldStatus, String newStatus,
                                   String performedBy, String notes) {
        DisputeTimeline entry = DisputeTimeline.builder()
                .disputeId(disputeId)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .notes(notes)
                .createdAt(OffsetDateTime.now())
                .build();
        timelineRepository.save(entry);
    }
}
