package com.switchplatform.platform.model.acquiring;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "total_amount", precision = 18, scale = 3)
    private BigDecimal totalAmount;

    @Column(name = "total_fee", precision = 18, scale = 3)
    private BigDecimal totalFee;

    @Column(name = "net_amount", precision = 18, scale = 3)
    private BigDecimal netAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(length = 20)
    private String status;

    @Column(name = "payment_ref", length = 64)
    private String paymentRef;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;
}
