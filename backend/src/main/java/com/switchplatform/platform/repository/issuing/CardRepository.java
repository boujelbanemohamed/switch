package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByCardNumberSuffix(String suffix);
    Optional<Card> findByCardNumberHash(String cardNumberHash);
    List<Card> findByCardholderId(UUID cardholderId);
}
