package com.switchplatform.platform.model.transfer;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    public enum TransferType { A2A, P2P }

    public enum TransferStatus { PENDING, COMPLETED, FAILED, REVERSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "source_reference", length = 64)
    private String sourceReference;

    @Column(name = "destination_reference", length = 64)
    private String destinationReference;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_currency", nullable = false, length = 3)
    @Builder.Default
    private String feeCurrency = "TND";

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ledger_journal_id")
    private UUID ledgerJournalId;

    @Column(name = "reversed_journal_id")
    private UUID reversedJournalId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String channel = "BACKOFFICE";

    @Column(name = "original_transfer_id")
    private UUID originalTransferId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
