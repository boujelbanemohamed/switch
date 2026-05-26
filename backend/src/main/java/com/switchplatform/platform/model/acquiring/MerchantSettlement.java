package com.switchplatform.platform.model.acquiring;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "total_transactions")
    private Integer totalTransactions = 0;

    @Column(name = "total_amount", precision = 18, scale = 3)
    private BigDecimal totalAmount;

    @Column(name = "total_fees", precision = 18, scale = 3)
    private BigDecimal totalFees;

    @Column(name = "total_commission", precision = 18, scale = 3)
    private BigDecimal totalCommission;

    @Column(name = "net_amount", precision = 18, scale = 3)
    private BigDecimal netAmount;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_reference", length = 64)
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum SettlementStatus {
        PENDING, CONFIRMED, PAID, DISPUTED, CANCELLED
    }
}
