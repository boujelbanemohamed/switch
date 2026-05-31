package com.switchplatform.platform.model.clearing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "interchange_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterchangeFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20)
    private String brand;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(length = 10)
    private String region;

    @Column(length = 4)
    private String mcc;

    @Column(name = "flat_fee", precision = 18, scale = 3)
    private BigDecimal flatFee;

    @Column(name = "percentage_fee", precision = 10, scale = 4)
    private BigDecimal percentageFee;
}
