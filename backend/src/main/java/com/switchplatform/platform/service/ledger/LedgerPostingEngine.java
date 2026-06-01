package com.switchplatform.platform.service.ledger;

import com.switchplatform.platform.model.ledger.*;
import com.switchplatform.platform.repository.ledger.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingEngine {

    private static final String SETTLEMENT_MAIN  = "SETTLEMENT_MAIN";
    private static final String INTERCHANGE_POOL = "INTERCHANGE_POOL";
    private static final String FEE_INCOME       = "FEE_INCOME";
    private static final String SUSPENSE         = "SUSPENSE";
    private static final String MERCHANT_ACQ     = "MERCHANT_ACQUIRER";
    private static final String HOLD_RESERVE     = "HOLD_RESERVE";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountingTransactionRepository accountingTransactionRepository;

    @Transactional
    public PostingResult postAuthorization(String transactionRef, BigDecimal amount,
                                           String currency, String description) {
        LedgerAccount suspense  = getAccount(SUSPENSE);
        LedgerAccount holdRes   = getAccount(HOLD_RESERVE);

        JournalEntry journal = JournalEntry.builder()
                .reference("AUTH-" + transactionRef)
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Authorization hold: " + description)
                .build();
        journal = journalEntryRepository.save(journal);

        UUID jId = journal.getId();
        ledgerEntryRepository.saveAll(List.of(
            entry(jId, suspense.getId(),  amount, null, currency, transactionRef, "Hold debit (suspense)"),
            entry(jId, holdRes.getId(),   null, amount, currency, transactionRef, "Hold credit (reserve)")
        ));

        suspense.setBalance(suspense.getBalance().add(amount));
        holdRes.setBalance(holdRes.getBalance().add(amount));
        ledgerAccountRepository.save(suspense);
        ledgerAccountRepository.save(holdRes);

        saveAccountingTxn(jId, "AUTHORIZATION", transactionRef);
        log.info("Ledger posted AUTHORIZATION {}: amount={}", transactionRef, amount);
        return new PostingResult(journal.getId(), "POSTED");
    }

    @Transactional
    public PostingResult postSettlement(String transactionRef, String merchantCode,
                                        BigDecimal netAmount, BigDecimal feeAmount,
                                        BigDecimal interchangeAmount, String currency) {
        LedgerAccount settlement = getAccount(SETTLEMENT_MAIN);
        LedgerAccount merchant   = getAccount(MERCHANT_ACQ);
        LedgerAccount fees       = getAccount(FEE_INCOME);
        LedgerAccount interchange = getAccount(INTERCHANGE_POOL);
        LedgerAccount suspense   = getAccount(SUSPENSE);
        LedgerAccount holdRes    = getAccount(HOLD_RESERVE);

        JournalEntry journal = JournalEntry.builder()
                .reference("STL-" + transactionRef)
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Settlement: merchant=" + merchantCode)
                .build();
        journal = journalEntryRepository.save(journal);
        UUID jId = journal.getId();

        BigDecimal total = netAmount.add(feeAmount).add(interchangeAmount);
        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(entry(jId, suspense.getId(),    null, total,           currency, transactionRef, "Release hold (suspense)"));
        entries.add(entry(jId, holdRes.getId(),      total, null,          currency, transactionRef, "Release hold (reserve)"));
        entries.add(entry(jId, merchant.getId(),     netAmount, null,      currency, transactionRef, "Merchant settlement"));
        entries.add(entry(jId, settlement.getId(),   null, netAmount,      currency, transactionRef, "Settlement credit"));
        entries.add(entry(jId, fees.getId(),         feeAmount, null,      currency, transactionRef, "Fee income"));
        entries.add(entry(jId, settlement.getId(),   null, feeAmount,      currency, transactionRef, "Fee settlement"));
        entries.add(entry(jId, interchange.getId(),  interchangeAmount, null, currency, transactionRef, "Intercharge receivable"));
        entries.add(entry(jId, settlement.getId(),   null, interchangeAmount, currency, transactionRef, "Interchange settlement"));

        ledgerEntryRepository.saveAll(entries);

        holdRes.setBalance(holdRes.getBalance().subtract(total));
        settlement.setBalance(settlement.getBalance().add(netAmount).add(feeAmount).add(interchangeAmount));
        merchant.setBalance(merchant.getBalance().add(netAmount));
        fees.setBalance(fees.getBalance().add(feeAmount));
        interchange.setBalance(interchange.getBalance().add(interchangeAmount));
        ledgerAccountRepository.saveAll(List.of(holdRes, settlement, merchant, fees, interchange));

        saveAccountingTxn(jId, "SETTLEMENT", transactionRef);
        log.info("Ledger posted SETTLEMENT {}: net={}, fee={}, interchange={}",
                transactionRef, netAmount, feeAmount, interchangeAmount);
        return new PostingResult(journal.getId(), "POSTED");
    }

    @Transactional
    public PostingResult postReversal(String originalTransactionRef, BigDecimal amount,
                                      String currency, String reason) {
        LedgerAccount suspense = getAccount(SUSPENSE);
        LedgerAccount holdRes  = getAccount(HOLD_RESERVE);

        JournalEntry journal = JournalEntry.builder()
                .reference("REV-" + originalTransactionRef)
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Reversal: " + reason)
                .build();
        journal = journalEntryRepository.save(journal);
        UUID jId = journal.getId();

        ledgerEntryRepository.saveAll(List.of(
            entry(jId, holdRes.getId(),   amount, null, currency, originalTransactionRef, "Reverse hold (reserve)"),
            entry(jId, suspense.getId(),  null, amount, currency, originalTransactionRef, "Reverse hold (suspense)")
        ));

        holdRes.setBalance(holdRes.getBalance().subtract(amount));
        suspense.setBalance(suspense.getBalance().subtract(amount));
        ledgerAccountRepository.save(holdRes);
        ledgerAccountRepository.save(suspense);

        saveAccountingTxn(jId, "REVERSAL", originalTransactionRef);
        log.info("Ledger posted REVERSAL {}: amount={}", originalTransactionRef, amount);
        return new PostingResult(journal.getId(), "POSTED");
    }

    @Transactional
    public PostingResult postFee(String transactionRef, BigDecimal feeAmount,
                                 String currency, String feeType) {
        LedgerAccount fees       = getAccount(FEE_INCOME);
        LedgerAccount settlement = getAccount(SETTLEMENT_MAIN);

        JournalEntry journal = JournalEntry.builder()
                .reference("FEE-" + transactionRef)
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Fee: " + feeType)
                .build();
        journal = journalEntryRepository.save(journal);
        UUID jId = journal.getId();

        ledgerEntryRepository.saveAll(List.of(
            entry(jId, settlement.getId(), feeAmount, null, currency, transactionRef, "Fee debit"),
            entry(jId, fees.getId(),       null, feeAmount, currency, transactionRef, "Fee credit")
        ));

        settlement.setBalance(settlement.getBalance().subtract(feeAmount));
        fees.setBalance(fees.getBalance().add(feeAmount));
        ledgerAccountRepository.save(settlement);
        ledgerAccountRepository.save(fees);

        saveAccountingTxn(jId, "FEE", transactionRef);
        log.info("Ledger posted FEE {}: amount={}, type={}", transactionRef, feeAmount, feeType);
        return new PostingResult(journal.getId(), "POSTED");
    }

    public PostingResult reverseEntry(UUID journalId, String reason) {
        JournalEntry original = journalEntryRepository.findById(journalId)
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + journalId));
        if (original.getStatus() == JournalEntry.JournalStatus.REVERSED) {
            throw new IllegalStateException("Journal already reversed: " + journalId);
        }

        List<LedgerEntry> originalEntries = ledgerEntryRepository.findByJournalId(journalId);
        JournalEntry reversal = JournalEntry.builder()
                .reference("RVRS-" + original.getReference())
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Reversal of " + original.getReference() + ": " + reason)
                .build();
        reversal = journalEntryRepository.save(reversal);
        UUID rId = reversal.getId();

        for (LedgerEntry e : originalEntries) {
            LedgerAccount account = ledgerAccountRepository.findById(e.getAccountId())
                    .orElseThrow();
            BigDecimal reverseDebit = e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO;
            BigDecimal reverseCredit = e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO;

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .journalId(rId).accountId(e.getAccountId())
                    .debitAmount(reverseDebit).creditAmount(reverseCredit)
                    .currency(e.getCurrency())
                    .transactionReference(e.getTransactionReference())
                    .description("Reversal: " + e.getDescription())
                    .build());

            BigDecimal delta = (reverseDebit != null ? reverseDebit : BigDecimal.ZERO)
                    .subtract(reverseCredit != null ? reverseCredit : BigDecimal.ZERO);
            account.setBalance(account.getBalance().add(delta));
            ledgerAccountRepository.save(account);
        }

        original.setStatus(JournalEntry.JournalStatus.REVERSED);
        journalEntryRepository.save(original);

        Optional<AccountingTransaction> at = accountingTransactionRepository.findByJournalId(journalId);
        at.ifPresent(t -> {
            t.setStatus(AccountingTransaction.AccountingStatus.REVERSED);
            accountingTransactionRepository.save(t);
        });

        log.info("Ledger reversed journal {}: {}", journalId, reason);
        return new PostingResult(reversal.getId(), "REVERSED");
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesForTransaction(String transactionRef) {
        return ledgerEntryRepository.findByTransactionReference(transactionRef);
    }

    private LedgerAccount getAccount(String accountNumber) {
        return ledgerAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + accountNumber));
    }

    private LedgerEntry entry(UUID journalId, UUID accountId, BigDecimal debit,
                              BigDecimal credit, String currency, String ref, String desc) {
        return LedgerEntry.builder()
                .journalId(journalId).accountId(accountId)
                .debitAmount(debit).creditAmount(credit)
                .currency(currency).transactionReference(ref)
                .description(desc).build();
    }

    private void saveAccountingTxn(UUID journalId, String type, String ref) {
        accountingTransactionRepository.save(AccountingTransaction.builder()
                .journalId(journalId).transactionType(type)
                .reference(ref).status(AccountingTransaction.AccountingStatus.POSTED)
                .build());
    }

    public record PostingResult(UUID journalId, String status) {}
}
