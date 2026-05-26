package com.switch.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bin_tables", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bin", "bin_length", "participant_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 19)
    private String bin;

    @Column(name = "bin_length", nullable = false)
    private Integer binLength = 6;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "card_brand", length = 20)
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand;

    @Column(name = "card_type", length = 20)
    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum CardBrand { VISA, MASTERCARD, AMEX, CB, OTHER }
    public enum CardType { CREDIT, DEBIT, PREPAID, CHARGE }
}
