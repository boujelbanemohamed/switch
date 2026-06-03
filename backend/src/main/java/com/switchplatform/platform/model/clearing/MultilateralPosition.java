package com.switchplatform.platform.model.clearing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "multilateral_positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultilateralPosition {

    public enum PositionType {
        DEBIT, CREDIT, NEUTRAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "gross_debit", nullable = false, precision = 18, scale = 3)
    private BigDecimal grossDebit;

    @Column(name = "gross_credit", nullable = false, precision = 18, scale = 3)
    private BigDecimal grossCredit;

    @Column(name = "net_position", nullable = false, precision = 18, scale = 3)
    private BigDecimal netPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", nullable = false, length = 10)
    private PositionType positionType;

    @Column(name = "settlement_status", length = 20)
    private String settlementStatus;

    @Column(name = "settlement_reference", length = 64)
    private String settlementReference;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;
}
