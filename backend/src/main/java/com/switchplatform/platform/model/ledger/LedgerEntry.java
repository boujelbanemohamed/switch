package com.switchplatform.platform.model.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "debit_amount", precision = 18, scale = 3)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 18, scale = 3)
    private BigDecimal creditAmount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
