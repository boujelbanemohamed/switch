package com.switchplatform.platform.model.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounting_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "transaction_type", length = 32, nullable = false)
    private String transactionType;

    @Column(name = "reference", length = 64, nullable = false)
    private String reference;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountingStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) status = AccountingStatus.POSTED;
    }

    public enum AccountingStatus {
        PENDING, POSTED, REVERSED, FAILED
    }
}
