package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.VirtualCard;
import com.switchplatform.platform.model.issuing.VirtualCard.Status;
import com.switchplatform.platform.repository.issuing.VirtualCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualCardService {

    private final VirtualCardRepository virtualCardRepository;

    @Transactional
    public VirtualCard createVirtualCard(VirtualCard card) {
        if (card.getUsageType() == VirtualCard.UsageType.SINGLE_USE) {
            card.setMaxTransactions(1);
        }
        card.setStatus(Status.PENDING_ACTIVATION);
        card.setAmountUsed(BigDecimal.ZERO);
        card.setTransactionCount(0);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard activateCard(UUID id) {
        VirtualCard card = getCard(id);
        if (card.getStatus() != Status.PENDING_ACTIVATION) {
            throw new IllegalStateException("Cannot activate card in status: " + card.getStatus());
        }
        card.setStatus(Status.ACTIVE);
        card.setActivatedAt(OffsetDateTime.now());
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard suspendCard(UUID id) {
        VirtualCard card = getCard(id);
        if (card.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Only active cards can be suspended");
        }
        card.setStatus(Status.SUSPENDED);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard resumeCard(UUID id) {
        VirtualCard card = getCard(id);
        if (card.getStatus() != Status.SUSPENDED) {
            throw new IllegalStateException("Only suspended cards can be resumed");
        }
        card.setStatus(Status.ACTIVE);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard cancelCard(UUID id, String reason) {
        VirtualCard card = getCard(id);
        card.setStatus(Status.CANCELLED);
        card.setCancelledAt(OffsetDateTime.now());
        card.setCancelReason(reason);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard expireCard(UUID id) {
        VirtualCard card = getCard(id);
        card.setStatus(Status.EXPIRED);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard recordUsage(UUID id, BigDecimal amount) {
        VirtualCard card = getCard(id);
        if (card.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Card is not active: " + card.getStatus());
        }
        if (card.getExpiresAt() != null && card.getExpiresAt().isBefore(OffsetDateTime.now())) {
            card.setStatus(Status.EXPIRED);
            return virtualCardRepository.save(card);
        }
        BigDecimal newUsed = card.getAmountUsed().add(amount);
        int newCount = card.getTransactionCount() + 1;

        card.setAmountUsed(newUsed);
        card.setTransactionCount(newCount);

        if (card.getAmountLimit() != null && newUsed.compareTo(card.getAmountLimit()) >= 0) {
            if (card.getUsageType() == VirtualCard.UsageType.SINGLE_USE) {
                card.setStatus(Status.CONSUMED);
            }
        }
        if (card.getMaxTransactions() != null && newCount >= card.getMaxTransactions()) {
            card.setStatus(Status.CONSUMED);
        }
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard updateLimits(UUID id, BigDecimal amountLimit, Integer maxTransactions) {
        VirtualCard card = getCard(id);
        if (card.getStatus() == Status.ACTIVE || card.getStatus() == Status.SUSPENDED) {
            if (amountLimit != null) card.setAmountLimit(amountLimit);
            if (maxTransactions != null) card.setMaxTransactions(maxTransactions);
        }
        return virtualCardRepository.save(card);
    }

    public VirtualCard getCard(UUID id) {
        return virtualCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Virtual card not found: " + id));
    }

    public VirtualCard getCardByExternalId(String externalId) {
        return virtualCardRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual card not found: " + externalId));
    }

    public List<VirtualCard> getCardsByCardholder(UUID cardholderId) {
        return virtualCardRepository.findByCardholderId(cardholderId);
    }

    public List<VirtualCard> getCardsByFundingCard(UUID fundingCardId) {
        return virtualCardRepository.findByFundingCardId(fundingCardId);
    }

    public List<VirtualCard> listAll() {
        return virtualCardRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Page<VirtualCard> listAll(int page, int size) {
        return virtualCardRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<VirtualCard> listByStatus(VirtualCard.Status status) {
        return virtualCardRepository.findByStatus(status);
    }
}
