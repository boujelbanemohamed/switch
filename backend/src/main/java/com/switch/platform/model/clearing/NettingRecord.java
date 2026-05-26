package com.switch.platform.model.clearing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "netting_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NettingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "netting_date", nullable = false)
    private LocalDate nettingDate;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "counterparty_id", nullable = false)
    private UUID counterpartyId;

    @Column(name = "total_sent", precision = 18, scale = 3)
    private BigDecimal totalSent;

    @Column(name = "total_received", precision = 18, scale = 3)
    private BigDecimal totalReceived;

    @Column(name = "net_amount", precision = 18, scale = 3)
    private BigDecimal netAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Column(name = "settlement_reference", length = 64)
    private String settlementReference;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum Status {
        PENDING, CONFIRMED, SETTLED, DISPUTED
    }
}
