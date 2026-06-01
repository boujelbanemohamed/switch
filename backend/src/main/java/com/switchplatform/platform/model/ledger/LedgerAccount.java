package com.switchplatform.platform.model.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", length = 34, nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "account_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "balance", precision = 18, scale = 3)
    private BigDecimal balance;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = AccountStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum AccountType {
        ASSET, LIABILITY, EQUITY, INCOME, EXPENSE, CONTINGENT
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, CLOSED
    }
}
