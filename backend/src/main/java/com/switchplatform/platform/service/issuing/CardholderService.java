package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.Cardholder;
import com.switchplatform.platform.repository.BinTableRepository;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.issuing.CardholderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardholderService {

    private final CardholderRepository cardholderRepository;
    private final CardService cardService;
    private final ParticipantRepository participantRepository;
    private final BinTableRepository binTableRepository;

    @Transactional
    public Cardholder createCardholder(Cardholder cardholder) {
        if (cardholder.getEmail() != null && cardholderRepository.existsByEmail(cardholder.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + cardholder.getEmail());
        }
        if (cardholder.getParticipantId() != null) {
            Participant participant = participantRepository.findById(cardholder.getParticipantId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + cardholder.getParticipantId()));
            cardholder.setParticipant(participant);
        }
        cardholder.setStatus(Cardholder.CardholderStatus.ACTIVE);
        cardholder.setKycLevel(1);
        cardholder.setCreatedAt(OffsetDateTime.now());
        cardholder.setUpdatedAt(OffsetDateTime.now());

        Cardholder saved = cardholderRepository.save(cardholder);
        log.info("Created cardholder {} {} {} (participant: {})",
                saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getParticipant() != null ? saved.getParticipant().getName() : "none");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Cardholder> listAll() {
        return cardholderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Page<Cardholder> listAll(int page, int size) {
        return cardholderRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Optional<Cardholder> getCardholder(UUID id) {
        return cardholderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Cardholder> getCardholderByEmail(String email) {
        return cardholderRepository.findByEmail(email);
    }

    @Transactional
    public Cardholder updateCardholder(UUID id, Cardholder update) {
        Cardholder existing = cardholderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cardholder not found: " + id));
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getFirstName() != null) existing.setFirstName(update.getFirstName());
        if (update.getLastName() != null) existing.setLastName(update.getLastName());
        if (update.getDateOfBirth() != null) existing.setDateOfBirth(update.getDateOfBirth());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getMobile() != null) existing.setMobile(update.getMobile());
        if (update.getAddressLine1() != null) existing.setAddressLine1(update.getAddressLine1());
        if (update.getAddressLine2() != null) existing.setAddressLine2(update.getAddressLine2());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getPostalCode() != null) existing.setPostalCode(update.getPostalCode());
        if (update.getCountryCode() != null) existing.setCountryCode(update.getCountryCode());
        if (update.getNationality() != null) existing.setNationality(update.getNationality());
        if (update.getIdDocumentType() != null) existing.setIdDocumentType(update.getIdDocumentType());
        if (update.getIdDocumentNumber() != null) existing.setIdDocumentNumber(update.getIdDocumentNumber());
        if (update.getParticipantId() != null) {
            Participant participant = participantRepository.findById(update.getParticipantId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + update.getParticipantId()));
            existing.setParticipant(participant);
        }

        if (update.getEmail() != null && !update.getEmail().equals(existing.getEmail())) {
            if (cardholderRepository.existsByEmail(update.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + update.getEmail());
            }
            existing.setEmail(update.getEmail());
        }

        existing.setUpdatedAt(OffsetDateTime.now());
        Cardholder saved = cardholderRepository.save(existing);
        log.info("Updated cardholder {}", id);
        return saved;
    }

    @Transactional
    public Cardholder updateKycLevel(UUID id, int level) {
        Cardholder cardholder = cardholderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cardholder not found: " + id));
        cardholder.setKycLevel(level);
        cardholder.setUpdatedAt(OffsetDateTime.now());
        Cardholder saved = cardholderRepository.save(cardholder);
        log.info("Updated KYC level for cardholder {} to {}", id, level);
        return saved;
    }

    @Transactional
    public Cardholder blockCardholder(UUID id) {
        Cardholder cardholder = cardholderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cardholder not found: " + id));
        cardholder.setStatus(Cardholder.CardholderStatus.BLOCKED);
        cardholder.setUpdatedAt(OffsetDateTime.now());
        Cardholder saved = cardholderRepository.save(cardholder);
        log.info("Blocked cardholder {}", id);

        // Block all cards for this cardholder
        getCardsForCardholder(id).forEach(cardId ->
                cardService.blockCard(cardId, "CARDHOLDER_BLOCKED"));
        log.info("Blocked all cards for cardholder {}", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BinTable> getBinsForCardholder(UUID cardholderId) {
        Cardholder cardholder = cardholderRepository.findById(cardholderId)
                .orElseThrow(() -> new IllegalArgumentException("Cardholder not found: " + cardholderId));
        Participant participant = cardholder.getParticipant();
        if (participant == null) {
            return List.of();
        }
        return binTableRepository.findByParticipantId(participant.getId());
    }

    @Transactional(readOnly = true)
    public List<UUID> getCardsForCardholder(UUID cardholderId) {
        return cardService.getCardsByCardholderId(cardholderId).stream()
                .map(Card::getId)
                .collect(Collectors.toList());
    }
}
