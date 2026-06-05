package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.CreditLine;
import com.switchplatform.platform.model.credit.CreditStatement;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.model.ledger.LedgerAccount;
import com.switchplatform.platform.model.ledger.LedgerEntry;
import com.switchplatform.platform.model.ledger.JournalEntry;
import com.switchplatform.platform.repository.credit.CreditLineRepository;
import com.switchplatform.platform.repository.credit.CreditStatementRepository;
import com.switchplatform.platform.repository.issuing.CardAccountRepository;
import com.switchplatform.platform.repository.ledger.AccountingTransactionRepository;
import com.switchplatform.platform.repository.ledger.JournalEntryRepository;
import com.switchplatform.platform.repository.ledger.LedgerAccountRepository;
import com.switchplatform.platform.repository.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditLineServiceTest {

    @Mock private CreditLineRepository creditLineRepository;
    @Mock private CardAccountRepository cardAccountRepository;
    @Mock private CreditStatementRepository creditStatementRepository;
    @Mock private LedgerAccountRepository ledgerAccountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private AccountingTransactionRepository accountingTransactionRepository;

    private CreditLineService service;
    private CreditLine line;
    private UUID lineId;

    @BeforeEach
    void setUp() {
        service = new CreditLineService(creditLineRepository, cardAccountRepository,
                creditStatementRepository, ledgerAccountRepository,
                journalEntryRepository, ledgerEntryRepository,
                accountingTransactionRepository);

        lineId = UUID.randomUUID();
        line = CreditLine.builder()
                .id(lineId)
                .cardAccountId(UUID.randomUUID())
                .creditLimit(BigDecimal.valueOf(5000))
                .currentBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .availableCredit(BigDecimal.valueOf(5000))
                .apr(new BigDecimal("18.00"))
                .currencyCode("TND")
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();
    }

    @Test
    void authorize_withinLimit_shouldReduceAvailableCredit() {
        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditLineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditLine result = service.authorize(lineId, BigDecimal.valueOf(1000));

        assertEquals(BigDecimal.valueOf(1000), result.getHoldAmount());
        assertEquals(BigDecimal.valueOf(4000), result.getAvailableCredit());
    }

    @Test
    void authorize_exceedingLimit_shouldThrow() {
        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));

        assertThrows(IllegalStateException.class,
                () -> service.authorize(lineId, BigDecimal.valueOf(6000)));
    }

    @Test
    void postPurchase_thenPostPayment_shouldLeaveZeroBalance() {
        LedgerAccount receivable = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("CREDIT_RECEIVABLE")
                .accountType(LedgerAccount.AccountType.ASSET).currency("TND")
                .balance(BigDecimal.ZERO).build();
        LedgerAccount funding = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("CREDIT_FUNDING")
                .accountType(LedgerAccount.AccountType.LIABILITY).currency("TND")
                .balance(BigDecimal.ZERO).build();
        JournalEntry journal = new JournalEntry();
        journal.setId(UUID.randomUUID());

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditLineRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(ledgerAccountRepository.findByAccountNumber("CREDIT_RECEIVABLE")).thenReturn(Optional.of(receivable));
        when(ledgerAccountRepository.findByAccountNumber("CREDIT_FUNDING")).thenReturn(Optional.of(funding));
        when(journalEntryRepository.save(any())).thenReturn(journal);
        when(ledgerEntryRepository.saveAll(any())).thenReturn(null);
        when(accountingTransactionRepository.save(any())).thenReturn(null);

        // Purchase: 2000
        service.postPurchase(lineId, BigDecimal.valueOf(2000), "TXN001");
        assertEquals(BigDecimal.valueOf(2000), line.getCurrentBalance());

        // Payment: 2000
        when(creditStatementRepository.findTopByCreditLineIdOrderByStatementDateDesc(lineId))
                .thenReturn(Optional.empty());
        service.postPayment(lineId, BigDecimal.valueOf(2000));
        assertEquals(BigDecimal.ZERO, line.getCurrentBalance());
        assertEquals(BigDecimal.valueOf(5000), line.getAvailableCredit());
    }

    @Test
    void ledgerEntries_shouldBeBalanced() {
        LedgerAccount receivable = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("CREDIT_RECEIVABLE")
                .accountType(LedgerAccount.AccountType.ASSET).currency("TND")
                .balance(BigDecimal.ZERO).build();
        LedgerAccount funding = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("CREDIT_FUNDING")
                .accountType(LedgerAccount.AccountType.LIABILITY).currency("TND")
                .balance(BigDecimal.ZERO).build();
        JournalEntry journal = new JournalEntry();
        journal.setId(UUID.randomUUID());

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditLineRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(ledgerAccountRepository.findByAccountNumber("CREDIT_RECEIVABLE")).thenReturn(Optional.of(receivable));
        when(ledgerAccountRepository.findByAccountNumber("CREDIT_FUNDING")).thenReturn(Optional.of(funding));
        when(journalEntryRepository.save(any())).thenReturn(journal);
        when(ledgerEntryRepository.saveAll(any())).thenReturn(null);
        when(accountingTransactionRepository.save(any())).thenReturn(null);

        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.captor();
        service.postPurchase(lineId, BigDecimal.valueOf(1500), "TXN002");

        verify(ledgerEntryRepository, times(1)).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(2, entries.size());

        BigDecimal totalDebit = entries.stream()
                .map(e -> e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream()
                .map(e -> e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(totalDebit, totalCredit, "Debit must equal credit in double-entry posting");
        assertEquals(BigDecimal.valueOf(1500), totalDebit);
    }
}
