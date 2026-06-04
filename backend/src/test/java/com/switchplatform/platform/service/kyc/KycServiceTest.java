package com.switchplatform.platform.service.kyc;

import com.switchplatform.platform.model.issuing.Cardholder;
import com.switchplatform.platform.model.kyc.KycDocument;
import com.switchplatform.platform.model.kyc.KycVerification;
import com.switchplatform.platform.repository.issuing.CardholderRepository;
import com.switchplatform.platform.repository.kyc.KycDocumentRepository;
import com.switchplatform.platform.repository.kyc.KycVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KycServiceTest {

    private KycService service;
    private KycDocumentRepository documentRepo;
    private KycVerificationRepository verificationRepo;
    private CardholderRepository cardholderRepo;
    private final Map<UUID, KycDocument> docStore = new ConcurrentHashMap<>();
    private final Map<UUID, KycVerification> verifStore = new ConcurrentHashMap<>();
    private final Map<UUID, Cardholder> cardholderStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        docStore.clear();
        verifStore.clear();
        cardholderStore.clear();

        documentRepo = mock(KycDocumentRepository.class);
        verificationRepo = mock(KycVerificationRepository.class);
        cardholderRepo = mock(CardholderRepository.class);

        when(documentRepo.save(any())).thenAnswer(inv -> {
            KycDocument d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            if (d.getVerificationStatus() == null) d.setVerificationStatus(KycDocument.VerificationStatus.PENDING);
            if (d.getCreatedAt() == null) d.setCreatedAt(java.time.OffsetDateTime.now());
            if (d.getUpdatedAt() == null) d.setUpdatedAt(java.time.OffsetDateTime.now());
            docStore.put(d.getId(), d);
            return d;
        });
        when(documentRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(docStore.get(inv.getArgument(0))));
        when(documentRepo.findByCardholderIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> {
            UUID chId = inv.getArgument(0);
            return docStore.values().stream()
                    .filter(d -> chId.equals(d.getCardholderId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        });
        when(documentRepo.findByVerificationStatus(any())).thenAnswer(inv -> {
            KycDocument.VerificationStatus vs = inv.getArgument(0);
            return docStore.values().stream().filter(d -> vs == d.getVerificationStatus()).toList();
        });

        when(verificationRepo.save(any())).thenAnswer(inv -> {
            KycVerification v = inv.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            if (v.getCreatedAt() == null) v.setCreatedAt(java.time.OffsetDateTime.now());
            if (v.getUpdatedAt() == null) v.setUpdatedAt(java.time.OffsetDateTime.now());
            verifStore.put(v.getId(), v);
            return v;
        });
        when(verificationRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(verifStore.get(inv.getArgument(0))));
        when(verificationRepo.findByCardholderIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> {
            UUID chId = inv.getArgument(0);
            return verifStore.values().stream()
                    .filter(v -> chId.equals(v.getCardholderId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        });
        when(verificationRepo.findByStatus(any())).thenAnswer(inv -> {
            KycVerification.Status s = inv.getArgument(0);
            return verifStore.values().stream().filter(v -> s == v.getStatus()).toList();
        });

        when(cardholderRepo.save(any())).thenAnswer(inv -> {
            Cardholder ch = inv.getArgument(0);
            if (ch.getId() == null) ch.setId(UUID.randomUUID());
            cardholderStore.put(ch.getId(), ch);
            return ch;
        });
        when(cardholderRepo.existsById(any())).thenReturn(true);
        when(cardholderRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(cardholderStore.get(inv.getArgument(0))));

        service = new KycService(documentRepo, verificationRepo, cardholderRepo);
    }

    @Test
    void shouldUploadDocument() {
        KycDocument doc = KycDocument.builder()
                .cardholderId(UUID.randomUUID())
                .documentType("PASSPORT")
                .documentNumber("AB123456")
                .build();
        KycDocument saved = service.uploadDocument(doc);
        assertNotNull(saved.getId());
        assertEquals(KycDocument.VerificationStatus.PENDING, saved.getVerificationStatus());
    }

    @Test
    void shouldVerifyDocument() {
        KycDocument doc = service.uploadDocument(KycDocument.builder()
                .cardholderId(UUID.randomUUID()).documentType("ID").build());
        KycDocument verified = service.verifyDocument(doc.getId(), true, "admin", null);
        assertEquals(KycDocument.VerificationStatus.VERIFIED, verified.getVerificationStatus());
        assertEquals("admin", verified.getVerifiedBy());
        assertNotNull(verified.getVerifiedAt());
    }

    @Test
    void shouldRejectDocument() {
        KycDocument doc = service.uploadDocument(KycDocument.builder()
                .cardholderId(UUID.randomUUID()).documentType("ID").build());
        KycDocument rejected = service.verifyDocument(doc.getId(), false, "admin", "Invalid");
        assertEquals(KycDocument.VerificationStatus.REJECTED, rejected.getVerificationStatus());
        assertEquals("Invalid", rejected.getRejectionReason());
    }

    @Test
    void shouldThrowWhenDocumentNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyDocument(UUID.randomUUID(), true, null, null));
    }

    @Test
    void shouldStartVerification() {
        KycVerification v = service.startVerification(
                UUID.randomUUID(), KycVerification.VerificationType.IDENTITY, 2);
        assertNotNull(v.getId());
        assertEquals(KycVerification.Status.IN_PROGRESS, v.getStatus());
        assertEquals(2, v.getRequestedLevel());
    }

    @Test
    void shouldCompleteVerificationAndUpgradeKycLevel() {
        Cardholder ch = new Cardholder();
        ch.setId(UUID.randomUUID());
        ch.setKycLevel(1);
        cardholderRepo.save(ch);

        KycVerification v = service.startVerification(
                ch.getId(), KycVerification.VerificationType.IDENTITY, 3);
        KycVerification completed = service.completeVerification(
                v.getId(), true, "admin", "All good", null);
        assertEquals(KycVerification.Status.VERIFIED, completed.getStatus());
        assertEquals("admin", completed.getVerifiedBy());

        Cardholder updated = cardholderRepo.findById(ch.getId()).orElseThrow();
        assertEquals(3, updated.getKycLevel().intValue());
    }

    @Test
    void shouldRejectVerification() {
        KycVerification v = service.startVerification(
                UUID.randomUUID(), KycVerification.VerificationType.ADDRESS, 1);
        KycVerification rejected = service.completeVerification(
                v.getId(), false, null, null, "Bad address");
        assertEquals(KycVerification.Status.REJECTED, rejected.getStatus());
        assertEquals("Bad address", rejected.getRejectionReason());
    }

    @Test
    void shouldGetDocuments() {
        UUID chId = UUID.randomUUID();
        service.uploadDocument(KycDocument.builder().cardholderId(chId).documentType("ID").build());
        service.uploadDocument(KycDocument.builder().cardholderId(chId).documentType("PASSPORT").build());
        assertEquals(2, service.getDocuments(chId).size());
    }

    @Test
    void shouldGetPendingDocuments() {
        service.uploadDocument(KycDocument.builder().cardholderId(UUID.randomUUID()).documentType("ID").build());
        assertEquals(1, service.getPendingDocuments().size());
    }

    @Test
    void shouldGetVerifications() {
        UUID chId = UUID.randomUUID();
        service.startVerification(chId, KycVerification.VerificationType.IDENTITY, 1);
        service.startVerification(chId, KycVerification.VerificationType.ADDRESS, 2);
        assertEquals(2, service.getVerifications(chId).size());
    }

    @Test
    void shouldGetPendingVerifications() {
        KycVerification v = KycVerification.builder()
                .cardholderId(UUID.randomUUID())
                .verificationType(KycVerification.VerificationType.EMAIL)
                .status(KycVerification.Status.PENDING)
                .requestedLevel(1)
                .build();
        verificationRepo.save(v);
        assertEquals(1, service.getPendingVerifications().size());
    }

    @Test
    void shouldThrowWhenVerificationNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.completeVerification(UUID.randomUUID(), true, null, null, null));
    }

    @Test
    void shouldThrowWhenCardholderNotFoundOnVerification() {
        KycVerification v = service.startVerification(
                UUID.randomUUID(), KycVerification.VerificationType.IDENTITY, 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.completeVerification(v.getId(), true, null, null, null));
    }
}
