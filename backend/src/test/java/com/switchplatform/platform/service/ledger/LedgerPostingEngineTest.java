package com.switchplatform.platform.service.ledger;

import com.switchplatform.platform.model.ledger.LedgerAccount;
import com.switchplatform.platform.model.ledger.LedgerEntry;
import com.switchplatform.platform.model.ledger.JournalEntry;
import com.switchplatform.platform.repository.ledger.AccountingTransactionRepository;
import com.switchplatform.platform.repository.ledger.JournalEntryRepository;
import com.switchplatform.platform.repository.ledger.LedgerAccountRepository;
import com.switchplatform.platform.repository.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerPostingEngineTest {

    @Mock private LedgerAccountRepository ledgerAccountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private AccountingTransactionRepository accountingTransactionRepository;

    private LedgerPostingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LedgerPostingEngine(ledgerAccountRepository, journalEntryRepository,
                ledgerEntryRepository, accountingTransactionRepository);
    }

    @Test
    void postAuthorization_shouldCreateDebitAndCreditEntries() {
        UUID accountId = UUID.randomUUID();
        LedgerAccount account = LedgerAccount.builder()
                .id(accountId)
                .accountNumber("HOLD_RESERVE")
                .accountType(LedgerAccount.AccountType.ASSET)
                .currency("TND")
                .balance(BigDecimal.valueOf(1000))
                .build();

        JournalEntry journal = new JournalEntry();
        journal.setId(UUID.randomUUID());
        journal.setReference("AUTH-txn1");

        when(ledgerAccountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(account));
        when(journalEntryRepository.save(any())).thenReturn(journal);
        when(ledgerEntryRepository.saveAll(any())).thenReturn(null);

        assertDoesNotThrow(() -> engine.postAuthorization("txn1", BigDecimal.valueOf(100), "TND", "test"));

        verify(ledgerEntryRepository, atLeast(1)).saveAll(any());
    }

    @Test
    void postAuthorization_shouldThrowWhenAccountNotFound() {
        when(ledgerAccountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> engine.postAuthorization("txn1", BigDecimal.valueOf(100), "TND", "test"));
    }
}
