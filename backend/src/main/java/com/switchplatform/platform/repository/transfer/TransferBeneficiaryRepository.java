package com.switchplatform.platform.repository.transfer;

import com.switchplatform.platform.model.transfer.TransferBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferBeneficiaryRepository extends JpaRepository<TransferBeneficiary, UUID> {
    List<TransferBeneficiary> findByOwnerCardholderIdOrderByCreatedAtDesc(UUID ownerCardholderId);
}
