package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.Cardholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardholderRepository extends JpaRepository<Cardholder, UUID> {
    Optional<Cardholder> findByExternalId(String externalId);
    Optional<Cardholder> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Cardholder> findByStatus(Cardholder.CardholderStatus status);
}
