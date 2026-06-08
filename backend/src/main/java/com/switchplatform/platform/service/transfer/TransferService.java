package com.switchplatform.platform.service.transfer;

import com.switchplatform.platform.config.TransferConfig;
import com.switchplatform.platform.config.TransferConfig.FeeConfig;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.model.ledger.JournalEntry;
import com.switchplatform.platform.model.ledger.LedgerAccount;
import com.switchplatform.platform.model.ledger.LedgerEntry;
import com.switchplatform.platform.model.transfer.Transfer;
import com.switchplatform.platform.model.transfer.TransferLimit;
import com.switchplatform.platform.repository.issuing.CardAccountRepository;
import com.switchplatform.platform.repository.issuing.CardRepository;
import com.switchplatform.platform.repository.ledger.JournalEntryRepository;
import com.switchplatform.platform.repository.ledger.LedgerAccountRepository;
import com.switchplatform.platform.repository.ledger.LedgerEntryRepository;
import com.switchplatform.platform.repository.transfer.TransferBeneficiaryRepository;
import com.switchplatform.platform.repository.transfer.TransferLimitRepository;
import com.switchplatform.platform.repository.transfer.TransferRepository;
import com.switchplatform.platform.service.issuing.CardAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private static final String TRANSFER_FEE_INCOME = "TRANSFER_FEE_INCOME";
    private static final String SETTLEMENT_MAIN = "SETTLEMENT_MAIN";
    private static final String DEFAULT_FEE_CURRENCY = "TND";

    private final TransferConfig transferConfig;
    private final TransferRepository transferRepository;
    private final TransferLimitRepository limitRepository;
    private final TransferBeneficiaryRepository beneficiaryRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CardRepository cardRepository;
    private final CardAccountService cardAccountService;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public Transfer executeA2A(UUID sourceAccountId, UUID destinationAccountId,
                               BigDecimal amount, String currency, String channel) {
        return executeTransfer(Transfer.TransferType.A2A, sourceAccountId, destinationAccountId,
                null, null, amount, currency, channel);
    }

    @Transactional
    public Transfer executeP2P(String sourcePan, String destinationRef,
                               BigDecimal amount, String currency, String channel) {
        CardAccount source = resolvePanToAccount(sourcePan);
        CardAccount destination = resolveDestinationRef(destinationRef);

        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Source and destination must be different");
        }

        return executeTransfer(Transfer.TransferType.P2P, source.getId(), destination.getId(),
                sourcePan, destinationRef, amount, currency, channel);
    }

    @Transactional
    public Transfer reverseTransfer(UUID transferId, String reason) {
        Transfer original = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        if (original.getStatus() != Transfer.TransferStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reverse transfer in status: " + original.getStatus());
        }

        if (original.getOriginalTransferId() != null) {
            throw new IllegalStateException("Cannot reverse a reversal");
        }

        UUID journalId = original.getLedgerJournalId();
        if (journalId != null) {
            JournalEntry journal = journalEntryRepository.findById(journalId)
                    .orElseThrow(() -> new IllegalStateException("Ledger journal not found: " + journalId));
            if (journal.getStatus() == JournalEntry.JournalStatus.REVERSED) {
                throw new IllegalStateException("Transfer already reversed");
            }
        }

        cardAccountService.credit(original.getSourceAccountId(), original.getAmount().add(original.getFeeAmount()),
                original.getCurrencyCode());
        cardAccountService.debit(original.getDestinationAccountId(), original.getAmount(),
                original.getCurrencyCode());

        OffsetDateTime now = OffsetDateTime.now();
        Transfer reversal = Transfer.builder()
                .transferType(original.getTransferType())
                .sourceAccountId(original.getDestinationAccountId())
                .destinationAccountId(original.getSourceAccountId())
                .amount(original.getAmount())
                .currencyCode(original.getCurrencyCode())
                .feeAmount(BigDecimal.ZERO)
                .status(Transfer.TransferStatus.COMPLETED)
                .channel(original.getChannel())
                .originalTransferId(original.getId())
                .completedAt(now)
                .build();
        reversal = transferRepository.save(reversal);

        original.setStatus(Transfer.TransferStatus.REVERSED);
        if (journalId != null) {
            original.setReversedJournalId(journalId);
        }
        transferRepository.save(original);

        log.info("Transfer reversed: original={}, reversal={}, reason={}", transferId, reversal.getId(), reason);
        return reversal;
    }

    // ---------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------

    private Transfer executeTransfer(Transfer.TransferType type, UUID sourceAccountId,
                                     UUID destinationAccountId, String sourceRef, String destRef,
                                     BigDecimal amount, String currency, String channel) {
        CardAccount source = cardAccountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountId));
        CardAccount destination = cardAccountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + destinationAccountId));

        validateAccounts(source, destination, currency);

        BigDecimal feeAmount = computeFee(type, amount);
        BigDecimal totalDebit = amount.add(feeAmount);

        if (source.getAvailableBalance().compareTo(totalDebit) < 0) {
            throw new IllegalStateException("Insufficient available balance: " + source.getAvailableBalance()
                    + " < " + totalDebit);
        }

        checkLimits(type, sourceAccountId, amount, currency);

        cardAccountService.debit(source.getId(), totalDebit, currency);
        cardAccountService.credit(destination.getId(), amount, currency);

        UUID feeJournalId = null;
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            feeJournalId = postFeeLedger(feeAmount, currency, type.name() + " transfer fee");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Transfer transfer = Transfer.builder()
                .transferType(type)
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .sourceReference(sourceRef)
                .destinationReference(destRef)
                .amount(amount)
                .currencyCode(currency)
                .feeAmount(feeAmount)
                .feeCurrency(DEFAULT_FEE_CURRENCY)
                .status(Transfer.TransferStatus.COMPLETED)
                .ledgerJournalId(feeJournalId)
                .channel(channel)
                .completedAt(now)
                .build();
        transfer = transferRepository.save(transfer);

        log.info("Transfer {}: from={} to={} amount={} fee={} status=COMPLETED",
                transfer.getId(), sourceAccountId, destinationAccountId, amount, feeAmount);
        return transfer;
    }

    private void validateAccounts(CardAccount source, CardAccount destination, String currency) {
        if (source.getStatus() != CardAccount.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active: " + source.getStatus());
        }
        if (destination.getStatus() != CardAccount.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Destination account is not active: " + destination.getStatus());
        }
        if (!source.getCurrencyCode().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Source account currency mismatch: expected " + currency
                    + ", got " + source.getCurrencyCode());
        }
        if (!destination.getCurrencyCode().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Destination account currency mismatch: expected " + currency
                    + ", got " + destination.getCurrencyCode());
        }
    }

    private BigDecimal computeFee(Transfer.TransferType type, BigDecimal amount) {
        FeeConfig cfg = type == Transfer.TransferType.A2A ? transferConfig.getA2a() : transferConfig.getP2p();
        BigDecimal fixedFee = cfg.getFixed() != null ? cfg.getFixed() : BigDecimal.ZERO;
        BigDecimal pctFee = BigDecimal.ZERO;
        if (cfg.getPercent() != null && cfg.getPercent().compareTo(BigDecimal.ZERO) > 0) {
            pctFee = amount.multiply(cfg.getPercent()).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
        }
        return fixedFee.add(pctFee);
    }

    private void checkLimits(Transfer.TransferType type, UUID sourceAccountId,
                             BigDecimal amount, String currency) {
        Optional<TransferLimit> limitOpt = limitRepository.findByTransferTypeAndStatus(type.name(), "ACTIVE");
        if (limitOpt.isEmpty()) return;

        TransferLimit limit = limitOpt.get();
        if (amount.compareTo(limit.getPerTransferMax()) > 0) {
            throw new IllegalStateException("Transfer amount " + amount + " exceeds per-transfer limit "
                    + limit.getPerTransferMax());
        }

        LocalDate today = LocalDate.now();
        BigDecimal dailyAmount = transferRepository.sumDailyAmount(sourceAccountId, today, currency);
        if (dailyAmount.add(amount).compareTo(limit.getDailyMaxAmount()) > 0) {
            throw new IllegalStateException("Daily transfer amount " + dailyAmount
                    + " + " + amount + " exceeds daily max " + limit.getDailyMaxAmount());
        }

        Long dailyCount = transferRepository.countDailyTransfers(sourceAccountId, today);
        if (dailyCount >= limit.getDailyMaxCount()) {
            throw new IllegalStateException("Daily transfer count " + dailyCount
                    + " exceeds max " + limit.getDailyMaxCount());
        }
    }

    private UUID postFeeLedger(BigDecimal feeAmount, String currency, String description) {
        LedgerAccount feeIncome = ledgerAccountRepository.findByAccountNumber(TRANSFER_FEE_INCOME)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + TRANSFER_FEE_INCOME));
        LedgerAccount settlement = ledgerAccountRepository.findByAccountNumber(SETTLEMENT_MAIN)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + SETTLEMENT_MAIN));

        JournalEntry journal = journalEntryRepository.save(JournalEntry.builder()
                .reference("XFER-FEE-" + UUID.randomUUID().toString().substring(0, 8))
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description(description)
                .build());

        UUID jId = journal.getId();
        String ref = journal.getReference();
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.builder().journalId(jId).accountId(settlement.getId())
                        .debitAmount(feeAmount).creditAmount(BigDecimal.ZERO)
                        .currency(currency).transactionReference(ref)
                        .description("Transfer fee debit: " + description).build(),
                LedgerEntry.builder().journalId(jId).accountId(feeIncome.getId())
                        .debitAmount(BigDecimal.ZERO).creditAmount(feeAmount)
                        .currency(currency).transactionReference(ref)
                        .description("Transfer fee income: " + description).build()
        ));

        settlement.setBalance(settlement.getBalance().subtract(feeAmount));
        feeIncome.setBalance(feeIncome.getBalance().add(feeAmount));
        ledgerAccountRepository.save(settlement);
        ledgerAccountRepository.save(feeIncome);

        return jId;
    }

    private CardAccount resolvePanToAccount(String pan) {
        // In production, this would HMAC-hash the PAN and look up by hash
        // For now, find by suffix as a simplified approach
        String suffix;
        if (pan.length() <= 8) {
            suffix = pan;
        } else {
            suffix = pan.substring(pan.length() - 8);
        }
        var cardOpt = cardRepository.findByCardNumberSuffix(suffix);
        if (cardOpt.isEmpty()) {
            throw new IllegalArgumentException("Card not found for PAN suffix: " + suffix);
        }
        UUID cardholderId = cardOpt.get().getCardholderId();
        var accounts = cardAccountRepository.findByCardholderId(cardholderId);
        if (accounts.isEmpty()) {
            throw new IllegalArgumentException("No account found for cardholder: " + cardholderId);
        }
        return accounts.get(0);
    }

    private CardAccount resolveDestinationRef(String ref) {
        // Try as account number first, then as UUID
        try {
            UUID uuid = UUID.fromString(ref);
            return cardAccountRepository.findById(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found by UUID: " + ref));
        } catch (IllegalArgumentException e) {
            // Not a UUID, try account number
        }
        return cardAccountRepository.findByAccountNumber(ref)
                .orElseThrow(() -> new IllegalArgumentException("Account not found by number: " + ref));
    }
}
