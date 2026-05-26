package com.switch.platform.service.issuing;

import com.switch.platform.model.issuing.Card;
import com.switch.platform.model.issuing.Cardholder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardholderService {

    private final ConcurrentMap<UUID, Cardholder> cardholderStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> emailIndex = new ConcurrentHashMap<>();
    private final CardService cardService;

    @Transactional
    public Cardholder createCardholder(Cardholder cardholder) {
        if (cardholder.getId() == null) {
            cardholder.setId(UUID.randomUUID());
        }
        if (cardholder.getEmail() != null && emailIndex.containsKey(cardholder.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + cardholder.getEmail());
        }
        cardholder.setStatus(Cardholder.CardholderStatus.ACTIVE);
        cardholder.setKycLevel("1");
        cardholder.setCreatedAt(OffsetDateTime.now());
        cardholder.setUpdatedAt(OffsetDateTime.now());

        cardholderStore.put(cardholder.getId(), cardholder);
        if (cardholder.getEmail() != null) {
            emailIndex.put(cardholder.getEmail(), cardholder.getId());
        }
        log.info("Created cardholder {} {} {}", cardholder.getId(), cardholder.getFirstName(), cardholder.getLastName());
        return cardholder;
    }

    @Transactional(readOnly = true)
    public Optional<Cardholder> getCardholder(UUID id) {
        return Optional.ofNullable(cardholderStore.get(id));
    }

    @Transactional(readOnly = true)
    public Optional<Cardholder> getCardholderByEmail(String email) {
        return Optional.ofNullable(emailIndex.get(email))
                .map(cardholderStore::get);
    }

    @Transactional
    public Cardholder updateCardholder(UUID id, Cardholder update) {
        Cardholder existing = getCardholderOrThrow(id);
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

        if (update.getEmail() != null && !update.getEmail().equals(existing.getEmail())) {
            if (emailIndex.containsKey(update.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + update.getEmail());
            }
            emailIndex.remove(existing.getEmail());
            emailIndex.put(update.getEmail(), existing.getId());
            existing.setEmail(update.getEmail());
        }

        existing.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated cardholder {}", id);
        return existing;
    }

    @Transactional
    public Cardholder updateKycLevel(UUID id, int level) {
        Cardholder cardholder = getCardholderOrThrow(id);
        String newLevel = String.valueOf(level);
        cardholder.setKycLevel(newLevel);
        cardholder.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated KYC level for cardholder {} to {}", id, level);
        return cardholder;
    }

    @Transactional
    public Cardholder blockCardholder(UUID id) {
        Cardholder cardholder = getCardholderOrThrow(id);
        cardholder.setStatus(Cardholder.CardholderStatus.BLOCKED);
        cardholder.setUpdatedAt(OffsetDateTime.now());
        log.info("Blocked cardholder {}", id);

        // Block all cards for this cardholder
        getCardsForCardholder(id).forEach(cardId ->
                cardService.blockCard(cardId, "CARDHOLDER_BLOCKED"));
        log.info("Blocked all cards for cardholder {}", id);
        return cardholder;
    }

    @Transactional(readOnly = true)
    public List<UUID> getCardsForCardholder(UUID cardholderId) {
        return cardService.getCardsByCardholderId(cardholderId).stream()
                .map(Card::getId)
                .collect(Collectors.toList());
    }

    private Cardholder getCardholderOrThrow(UUID id) {
        Cardholder cardholder = cardholderStore.get(id);
        if (cardholder == null) {
            throw new IllegalArgumentException("Cardholder not found: " + id);
        }
        return cardholder;
    }
}
