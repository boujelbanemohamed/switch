package com.switchplatform.platform.model.transfer;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_cardholder_id", nullable = false)
    private UUID ownerCardholderId;

    @Column(nullable = false, length = 50)
    private String alias;

    @Column(name = "masked_pan", length = 20)
    private String maskedPan;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(length = 34)
    private String iban;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
