package com.switchplatform.platform.model.transfer;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_type", nullable = false, length = 10)
    private String transferType;

    @Column(name = "per_transfer_max", nullable = false, precision = 18, scale = 3)
    private BigDecimal perTransferMax;

    @Column(name = "daily_max_amount", nullable = false, precision = 18, scale = 3)
    private BigDecimal dailyMaxAmount;

    @Column(name = "daily_max_count", nullable = false)
    @Builder.Default
    private Integer dailyMaxCount = 10;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "TND";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
