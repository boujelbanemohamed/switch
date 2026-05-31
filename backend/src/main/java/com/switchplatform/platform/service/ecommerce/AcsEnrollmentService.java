package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsCardEnrollment;
import com.switchplatform.platform.repository.ecommerce.AcsCardEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcsEnrollmentService {

    private final AcsCardEnrollmentRepository repository;

    @Transactional
    public AcsCardEnrollment enrollCard(UUID cardId, UUID cardholderId, UUID merchantId,
                                         String phoneNumber, String email,
                                         String cardBrand, String cardType) {
        AcsCardEnrollment enrollment = AcsCardEnrollment.builder()
                .cardId(cardId)
                .cardholderId(cardholderId)
                .merchantId(merchantId)
                .status(AcsCardEnrollment.Status.ENROLLED)
                .phoneNumber(phoneNumber)
                .email(email)
                .cardBrand(cardBrand)
                .cardType(cardType)
                .build();
        repository.save(enrollment);
        log.info("Card enrolled for ACS: cardId={}, enrollmentId={}", cardId, enrollment.getId());
        return enrollment;
    }

    @Transactional
    public AcsCardEnrollment activateEnrollment(UUID enrollmentId) {
        AcsCardEnrollment enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        enrollment.setStatus(AcsCardEnrollment.Status.ACTIVE);
        log.info("ACS enrollment activated: id={}", enrollmentId);
        return enrollment;
    }

    @Transactional
    public AcsCardEnrollment suspendEnrollment(UUID enrollmentId) {
        AcsCardEnrollment enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        enrollment.setStatus(AcsCardEnrollment.Status.SUSPENDED);
        log.info("ACS enrollment suspended: id={}", enrollmentId);
        return enrollment;
    }

    @Transactional
    public AcsCardEnrollment cancelEnrollment(UUID enrollmentId) {
        AcsCardEnrollment enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        enrollment.setStatus(AcsCardEnrollment.Status.CANCELLED);
        enrollment.setCanceledAt(OffsetDateTime.now());
        log.info("ACS enrollment cancelled: id={}", enrollmentId);
        return enrollment;
    }

    @Transactional(readOnly = true)
    public AcsCardEnrollment getEnrollment(UUID enrollmentId) {
        return repository.findById(enrollmentId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AcsCardEnrollment> getEnrollmentsByCard(UUID cardId) {
        return repository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<AcsCardEnrollment> getEnrollmentsByCardholder(UUID cardholderId) {
        return repository.findByCardholderId(cardholderId);
    }

    @Transactional(readOnly = true)
    public List<AcsCardEnrollment> getEnrollmentsByStatus(AcsCardEnrollment.Status status) {
        return repository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public boolean isCardEnrolled(UUID cardId) {
        return repository.countByCardIdAndStatus(cardId, AcsCardEnrollment.Status.ACTIVE) > 0
                || repository.countByCardIdAndStatus(cardId, AcsCardEnrollment.Status.ENROLLED) > 0;
    }
}
