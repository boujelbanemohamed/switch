package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, UUID> {

    List<VirtualCard> findByCardholderId(UUID cardholderId);

    List<VirtualCard> findByFundingCardId(UUID fundingCardId);

    List<VirtualCard> findByStatus(VirtualCard.Status status);

    Optional<VirtualCard> findByExternalId(String externalId);

    List<VirtualCard> findByCardholderIdAndStatus(UUID cardholderId, VirtualCard.Status status);
}
