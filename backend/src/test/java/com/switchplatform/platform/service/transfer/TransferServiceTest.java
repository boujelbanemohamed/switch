package com.switchplatform.platform.service.transfer;

import com.switchplatform.platform.config.TransferConfig;
import com.switchplatform.platform.config.TransferConfig.FeeConfig;
import com.switchplatform.platform.model.issuing.Card;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    @Mock private TransferConfig transferConfig;
    @Mock private TransferRepository transferRepository;
    @Mock private TransferLimitRepository limitRepository;
    @Mock private TransferBeneficiaryRepository beneficiaryRepository;
    @Mock private CardAccountRepository cardAccountRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardAccountService cardAccountService;
    @Mock private LedgerAccountRepository ledgerAccountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;

    private TransferService service;
    private CardAccount source;
    private CardAccount destination;
    private UUID sourceId;
    private UUID destId;

    private LedgerAccount feeIncome;
    private LedgerAccount settlement;
    private JournalEntry journal;

    private FeeConfig a2aFee;
    private FeeConfig p2pFee;

    @BeforeEach
    void setUp() {
        service = new TransferService(transferConfig, transferRepository, limitRepository,
                beneficiaryRepository, cardAccountRepository, cardRepository,
                cardAccountService, ledgerAccountRepository, journalEntryRepository,
                ledgerEntryRepository);

        sourceId = UUID.randomUUID();
        destId = UUID.randomUUID();

        source = CardAccount.builder()
                .id(sourceId)
                .cardholderId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(1000))
                .availableBalance(BigDecimal.valueOf(1000))
                .holdAmount(BigDecimal.ZERO)
                .currencyCode("TND")
                .status(CardAccount.AccountStatus.ACTIVE)
                .build();

        destination = CardAccount.builder()
                .id(destId)
                .cardholderId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(500))
                .availableBalance(BigDecimal.valueOf(500))
                .holdAmount(BigDecimal.ZERO)
                .currencyCode("TND")
                .status(CardAccount.AccountStatus.ACTIVE)
                .build();

        a2aFee = new FeeConfig();
        a2aFee.setFixed(new BigDecimal("5.00"));
        a2aFee.setPercent(BigDecimal.ZERO);
        a2aFee.setCurrency("TND");

        p2pFee = new FeeConfig();
        p2pFee.setFixed(new BigDecimal("2.00"));
        p2pFee.setPercent(BigDecimal.ZERO);
        p2pFee.setCurrency("TND");

        feeIncome = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("TRANSFER_FEE_INCOME")
                .accountType(LedgerAccount.AccountType.INCOME).currency("TND")
                .balance(BigDecimal.ZERO).build();
        settlement = LedgerAccount.builder()
                .id(UUID.randomUUID()).accountNumber("SETTLEMENT_MAIN")
                .accountType(LedgerAccount.AccountType.ASSET).currency("TND")
                .balance(BigDecimal.valueOf(10000)).build();
        journal = new JournalEntry();
        journal.setId(UUID.randomUUID());

        when(transferConfig.getA2a()).thenReturn(a2aFee);
        when(transferConfig.getP2p()).thenReturn(p2pFee);
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(ledgerAccountRepository.findByAccountNumber("TRANSFER_FEE_INCOME")).thenReturn(Optional.of(feeIncome));
        when(ledgerAccountRepository.findByAccountNumber("SETTLEMENT_MAIN")).thenReturn(Optional.of(settlement));
        when(journalEntryRepository.save(any())).thenReturn(journal);
        when(ledgerEntryRepository.saveAll(any())).thenReturn(null);
    }

    private static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertTrue(expected.compareTo(actual) == 0,
                "Expected " + expected + " but got " + actual);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static BigDecimal bd(int v) {
        return BigDecimal.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private static <T> T eqBigDecimal(BigDecimal expected) {
        return (T) argThat(a -> a instanceof BigDecimal bd && bd.compareTo(expected) == 0);
    }

    @Test
    void executeA2A_nominal_shouldDebitSourceCreditDestination() {
        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.empty());

        Transfer result = service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE");

        assertBigDecimalEquals(BigDecimal.valueOf(300), result.getAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(5), result.getFeeAmount());
        assertEquals(Transfer.TransferStatus.COMPLETED, result.getStatus());
        assertEquals(Transfer.TransferType.A2A, result.getTransferType());

        verify(cardAccountService).debit(eq(sourceId), eqBigDecimal(bd(305)), eq("TND"));
        verify(cardAccountService).credit(eq(destId), eqBigDecimal(bd(300)), eq("TND"));
    }

    @Test
    void executeA2A_feeConfig_shouldSupportPercent() {
        source.setAvailableBalance(BigDecimal.valueOf(2000));

        a2aFee.setFixed(new BigDecimal("1.00"));
        a2aFee.setPercent(new BigDecimal("0.50"));

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.empty());

        Transfer result = service.executeA2A(sourceId, destId, BigDecimal.valueOf(1000), "TND", "BACKOFFICE");

        assertBigDecimalEquals(BigDecimal.valueOf(6), result.getFeeAmount());
    }

    @Test
    void executeA2A_insufficientBalance_shouldThrow() {
        source.setAvailableBalance(BigDecimal.valueOf(10));

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE"));
        verify(cardAccountService, never()).debit(any(), any(), any());
        verify(cardAccountService, never()).credit(any(), any(), any());
    }

    @Test
    void executeA2A_destinationNotActive_shouldThrowAndNotDebitSource() {
        BigDecimal originalBalance = source.getAvailableBalance();
        destination.setStatus(CardAccount.AccountStatus.CLOSED);

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));

        assertThrows(IllegalStateException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE"));
        assertEquals(originalBalance, source.getAvailableBalance(), "Source balance MUST be unchanged on destination inactive");
        verify(cardAccountService, never()).debit(any(), any(), any());
        verify(cardAccountService, never()).credit(any(), any(), any());
    }

    @Test
    void executeA2A_destinationNotFound_shouldThrowAndNotDebitSource() {
        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE"));
        verify(cardAccountService, never()).debit(any(), any(), any());
        verify(cardAccountService, never()).credit(any(), any(), any());
    }

    @Test
    void executeA2A_dailyLimitExceeded_shouldThrow() {
        TransferLimit limit = TransferLimit.builder()
                .transferType("A2A")
                .perTransferMax(new BigDecimal("10000"))
                .dailyMaxAmount(new BigDecimal("50000"))
                .dailyMaxCount(10)
                .currencyCode("TND")
                .status("ACTIVE")
                .build();

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.of(limit));
        when(transferRepository.sumDailyAmount(sourceId, LocalDate.now(), "TND")).thenReturn(new BigDecimal("49000"));
        when(transferRepository.countDailyTransfers(sourceId, LocalDate.now())).thenReturn(5L);

        assertThrows(IllegalStateException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(2000), "TND", "BACKOFFICE"));
        verify(cardAccountService, never()).debit(any(), any(), any());
    }

    @Test
    void executeA2A_perTransferLimitExceeded_shouldThrow() {
        TransferLimit limit = TransferLimit.builder()
                .transferType("A2A")
                .perTransferMax(new BigDecimal("10000"))
                .dailyMaxAmount(new BigDecimal("50000"))
                .dailyMaxCount(10)
                .currencyCode("TND")
                .status("ACTIVE")
                .build();

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.of(limit));

        assertThrows(IllegalStateException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(15000), "TND", "BACKOFFICE"));
    }

    @Test
    void executeA2A_crossCurrency_shouldReject() {
        destination.setCurrencyCode("EUR");

        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));

        assertThrows(IllegalArgumentException.class,
                () -> service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE"));
    }

    @Test
    void executeA2A_ledgerEntries_shouldBeBalanced() {
        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("A2A", "ACTIVE")).thenReturn(Optional.empty());

        service.executeA2A(sourceId, destId, BigDecimal.valueOf(300), "TND", "BACKOFFICE");

        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.captor();
        verify(ledgerEntryRepository, times(1)).saveAll(captor.capture());
        List<LedgerEntry> entries = captor.getValue();
        assertEquals(2, entries.size());

        BigDecimal totalDebit = entries.stream()
                .map(e -> e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream()
                .map(e -> e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertBigDecimalEquals(totalDebit, totalCredit);
        assertBigDecimalEquals(BigDecimal.valueOf(5), totalDebit);
    }

    @Test
    void reverseTransfer_shouldRestoreBalances() {
        UUID transferId = UUID.randomUUID();
        Transfer original = Transfer.builder()
                .id(transferId)
                .transferType(Transfer.TransferType.A2A)
                .sourceAccountId(sourceId)
                .destinationAccountId(destId)
                .amount(BigDecimal.valueOf(300))
                .currencyCode("TND")
                .feeAmount(BigDecimal.valueOf(5))
                .status(Transfer.TransferStatus.COMPLETED)
                .build();

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(original));

        Transfer reversal = service.reverseTransfer(transferId, "Test reversal");

        assertNotNull(reversal);
        assertEquals(transferId, reversal.getOriginalTransferId());
        verify(cardAccountService).credit(eq(sourceId), eqBigDecimal(bd(305)), eq("TND"));
        verify(cardAccountService).debit(eq(destId), eqBigDecimal(bd(300)), eq("TND"));
    }

    @Test
    void executeP2P_nominal_shouldTransfer() {
        String suffix = "12345678";
        String destRef = destId.toString();

        Card card = Card.builder()
                .id(UUID.randomUUID()).cardholderId(source.getCardholderId())
                .cardNumberSuffix(suffix).build();

        when(cardRepository.findByCardNumberSuffix(suffix)).thenReturn(Optional.of(card));
        when(cardAccountRepository.findByCardholderId(card.getCardholderId())).thenReturn(List.of(source));
        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(cardAccountRepository.findById(destId)).thenReturn(Optional.of(destination));
        when(limitRepository.findByTransferTypeAndStatus("P2P", "ACTIVE")).thenReturn(Optional.empty());

        Transfer result = service.executeP2P(suffix, destRef, BigDecimal.valueOf(200), "TND", "BACKOFFICE");

        assertBigDecimalEquals(BigDecimal.valueOf(200), result.getAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(2), result.getFeeAmount());
        assertEquals(Transfer.TransferStatus.COMPLETED, result.getStatus());

        verify(cardAccountService).debit(eq(sourceId), eqBigDecimal(bd(202)), eq("TND"));
        verify(cardAccountService).credit(eq(destId), eqBigDecimal(bd(200)), eq("TND"));
    }

    @Test
    void executeP2P_sourceEqualsDestination_shouldThrow() {
        String suffix = "12345678";
        String destRef = sourceId.toString();

        Card card = Card.builder()
                .id(UUID.randomUUID()).cardholderId(source.getCardholderId())
                .cardNumberSuffix(suffix).build();

        when(cardRepository.findByCardNumberSuffix(suffix)).thenReturn(Optional.of(card));
        when(cardAccountRepository.findByCardholderId(card.getCardholderId())).thenReturn(List.of(source));
        when(cardAccountRepository.findById(sourceId)).thenReturn(Optional.of(source));

        assertThrows(IllegalArgumentException.class,
                () -> service.executeP2P(suffix, destRef, BigDecimal.valueOf(200), "TND", "BACKOFFICE"));
    }
}
