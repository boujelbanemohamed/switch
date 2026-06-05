package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.CreditLine;
import com.switchplatform.platform.model.credit.CreditStatement;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.model.ledger.*;
import com.switchplatform.platform.repository.credit.CreditLineRepository;
import com.switchplatform.platform.repository.credit.CreditStatementRepository;
import com.switchplatform.platform.repository.issuing.CardAccountRepository;
import com.switchplatform.platform.repository.ledger.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditLineService {

    private static final String CREDIT_RECEIVABLE = "CREDIT_RECEIVABLE";
    private static final String CREDIT_FUNDING    = "CREDIT_FUNDING";

    private final CreditLineRepository creditLineRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CreditStatementRepository creditStatementRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountingTransactionRepository accountingTransactionRepository;

    @Transactional
    public CreditLine openCreditLine(UUID cardAccountId, BigDecimal creditLimit,
                                     BigDecimal apr, int statementDay, int paymentDueDays,
                                     BigDecimal minPaymentPct, BigDecimal minPaymentFloor) {
        CardAccount cardAccount = cardAccountRepository.findById(cardAccountId)
                .orElseThrow(() -> new IllegalArgumentException("CardAccount not found: " + cardAccountId));

        // Check if a credit line already exists
        creditLineRepository.findByCardAccountId(cardAccountId).ifPresent(cl -> {
            throw new IllegalStateException("Credit line already exists for account: " + cardAccountId);
        });

        // Update card account type to CREDIT
        cardAccount.setAccountType(CardAccount.AccountType.CREDIT);
        cardAccountRepository.save(cardAccount);

        CreditLine creditLine = CreditLine.builder()
                .cardAccountId(cardAccountId)
                .creditLimit(creditLimit)
                .currentBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .availableCredit(creditLimit)
                .apr(apr)
                .statementDay(statementDay)
                .paymentDueDays(paymentDueDays)
                .minPaymentPct(minPaymentPct)
                .minPaymentFloor(minPaymentFloor)
                .currencyCode(cardAccount.getCurrencyCode())
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();

        creditLine = creditLineRepository.save(creditLine);
        log.info("Credit line opened: id={}, account={}, limit={}, apr={}%",
                creditLine.getId(), cardAccountId, creditLimit, apr);
        return creditLine;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditLine authorize(UUID creditLineId, BigDecimal amount) {
        CreditLine cl = creditLineRepository.findById(creditLineId)
                .orElseThrow(() -> new IllegalArgumentException("Credit line not found: " + creditLineId));

        if (cl.getAvailableCredit().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient available credit: requested " + amount
                    + ", available " + cl.getAvailableCredit());
        }

        cl.setHoldAmount(cl.getHoldAmount().add(amount));
        cl.setAvailableCredit(cl.getAvailableCredit().subtract(amount));
        cl = creditLineRepository.save(cl);

        log.info("Credit authorize: line={}, amount={}, hold={}, available={}",
                creditLineId, amount, cl.getHoldAmount(), cl.getAvailableCredit());
        return cl;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditLine postPurchase(UUID creditLineId, BigDecimal amount, String transactionRef) {
        CreditLine cl = creditLineRepository.findById(creditLineId)
                .orElseThrow(() -> new IllegalArgumentException("Credit line not found: " + creditLineId));

        BigDecimal holdRelease = cl.getHoldAmount().min(amount);
        cl.setCurrentBalance(cl.getCurrentBalance().add(amount));
        cl.setHoldAmount(cl.getHoldAmount().subtract(holdRelease));
        cl.setAvailableCredit(cl.getAvailableCredit().subtract(amount.subtract(holdRelease)));

        postLedgerPurchase(cl, amount, transactionRef);

        cl = creditLineRepository.save(cl);
        log.info("Credit purchase posted: line={}, amount={}, balance={}, available={}",
                creditLineId, amount, cl.getCurrentBalance(), cl.getAvailableCredit());
        return cl;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditLine postPayment(UUID creditLineId, BigDecimal amount) {
        CreditLine cl = creditLineRepository.findById(creditLineId)
                .orElseThrow(() -> new IllegalArgumentException("Credit line not found: " + creditLineId));

        BigDecimal payment = amount.min(cl.getCurrentBalance());
        cl.setCurrentBalance(cl.getCurrentBalance().subtract(payment));
        cl.setAvailableCredit(cl.getAvailableCredit().add(payment));

        postLedgerPayment(cl, payment);

        cl = creditLineRepository.save(cl);

        // Check if any OPEN statement is now fully paid
        if (cl.getCurrentBalance().compareTo(BigDecimal.ZERO) == 0) {
            Optional<CreditStatement> latest = creditStatementRepository
                    .findTopByCreditLineIdOrderByStatementDateDesc(creditLineId);
            latest.ifPresent(statement -> {
                if (!statement.getPaidInFull()) {
                    statement.setPaidInFull(true);
                    statement.setStatus(CreditStatement.StatementStatus.PAID);
                    creditStatementRepository.save(statement);
                }
            });
        }

        log.info("Credit payment posted: line={}, amount={}, balance={}, available={}",
                creditLineId, amount, cl.getCurrentBalance(), cl.getAvailableCredit());
        return cl;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditLine releaseHold(UUID creditLineId, BigDecimal amount) {
        CreditLine cl = creditLineRepository.findById(creditLineId)
                .orElseThrow(() -> new IllegalArgumentException("Credit line not found: " + creditLineId));

        BigDecimal release = amount.min(cl.getHoldAmount());
        cl.setHoldAmount(cl.getHoldAmount().subtract(release));
        cl.setAvailableCredit(cl.getAvailableCredit().add(release));
        cl = creditLineRepository.save(cl);

        log.info("Credit hold released: line={}, amount={}, available={}",
                creditLineId, amount, cl.getAvailableCredit());
        return cl;
    }

    public Optional<CreditLine> findByCardAccountId(UUID cardAccountId) {
        return creditLineRepository.findByCardAccountId(cardAccountId);
    }

    public Optional<CreditLine> findById(UUID id) {
        return creditLineRepository.findById(id);
    }

    // ---------------------------------------------------------------
    // Ledger posting
    // ---------------------------------------------------------------

    private void postLedgerPurchase(CreditLine cl, BigDecimal amount, String transactionRef) {
        LedgerAccount receivable = getAccount(CREDIT_RECEIVABLE);
        LedgerAccount funding = getAccount(CREDIT_FUNDING);

        JournalEntry journal = JournalEntry.builder()
                .reference("CR-PUR-" + transactionRef)
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Credit purchase: line=" + cl.getId() + ", ref=" + transactionRef)
                .build();
        journal = journalEntryRepository.save(journal);

        UUID jId = journal.getId();
        ledgerEntryRepository.saveAll(java.util.List.of(
            entry(jId, receivable.getId(), amount, null, cl.getCurrencyCode(), transactionRef, "Purchase debit (receivable)"),
            entry(jId, funding.getId(),    null, amount, cl.getCurrencyCode(), transactionRef, "Purchase credit (funding)")
        ));

        receivable.setBalance(receivable.getBalance().add(amount));
        funding.setBalance(funding.getBalance().add(amount));
        ledgerAccountRepository.save(receivable);
        ledgerAccountRepository.save(funding);

        saveAccountingTxn(jId, "CREDIT_PURCHASE", transactionRef);
    }

    private void postLedgerPayment(CreditLine cl, BigDecimal amount) {
        LedgerAccount receivable = getAccount(CREDIT_RECEIVABLE);
        LedgerAccount funding = getAccount(CREDIT_FUNDING);

        JournalEntry journal = JournalEntry.builder()
                .reference("CR-PAY-" + cl.getId() + "-" + System.currentTimeMillis())
                .postingDate(OffsetDateTime.now())
                .status(JournalEntry.JournalStatus.POSTED)
                .description("Credit payment: line=" + cl.getId())
                .build();
        journal = journalEntryRepository.save(journal);

        UUID jId = journal.getId();
        String ref = "PAY-" + cl.getId();
        ledgerEntryRepository.saveAll(java.util.List.of(
            entry(jId, funding.getId(),    amount, null, cl.getCurrencyCode(), ref, "Payment debit (funding)"),
            entry(jId, receivable.getId(), null, amount, cl.getCurrencyCode(), ref, "Payment credit (receivable)")
        ));

        funding.setBalance(funding.getBalance().subtract(amount));
        receivable.setBalance(receivable.getBalance().subtract(amount));
        ledgerAccountRepository.save(funding);
        ledgerAccountRepository.save(receivable);

        saveAccountingTxn(jId, "CREDIT_PAYMENT", ref);
    }

    private LedgerAccount getAccount(String accountNumber) {
        return ledgerAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + accountNumber));
    }

    private LedgerEntry entry(UUID journalId, UUID accountId, BigDecimal debit,
                              BigDecimal credit, String currency, String ref, String desc) {
        return LedgerEntry.builder()
                .journalId(journalId).accountId(accountId)
                .debitAmount(debit != null ? debit : BigDecimal.ZERO)
                .creditAmount(credit != null ? credit : BigDecimal.ZERO)
                .currency(currency).transactionReference(ref)
                .description(desc).build();
    }

    private void saveAccountingTxn(UUID journalId, String type, String ref) {
        accountingTransactionRepository.save(AccountingTransaction.builder()
                .journalId(journalId).transactionType(type)
                .reference(ref).status(AccountingTransaction.AccountingStatus.POSTED)
                .build());
    }
}
