package com.switchplatform.platform.model.acquiring;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "netting_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NettingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column
    private LocalDate date;

    @Column(name = "gross_amount", precision = 18, scale = 3)
    private BigDecimal grossAmount;

    @Column(name = "total_fees", precision = 18, scale = 3)
    private BigDecimal totalFees;

    @Column(name = "net_amount", precision = 18, scale = 3)
    private BigDecimal netAmount;

    @Column(name = "transaction_count")
    private int transactionCount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
