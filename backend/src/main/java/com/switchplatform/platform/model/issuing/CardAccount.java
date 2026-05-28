package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "account_number", length = 34, unique = true)
    private String accountNumber;

    @Column(length = 34)
    private String iban;

    @Column(name = "account_type", length = 20)
    @Enumerated(EnumType.STRING)
    private AccountType accountType = AccountType.CHECKING;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(precision = 18, scale = 3)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "ledger_balance", precision = 18, scale = 3)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 18, scale = 3)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "hold_amount", precision = 18, scale = 3)
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "opened_at")
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (openedAt == null) openedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum AccountType {
        CHECKING, SAVINGS, PREPAID, CREDIT, LOAN
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, BLOCKED, CLOSED
    }
}
