package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.CardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardAccountRepository extends JpaRepository<CardAccount, UUID> {
    List<CardAccount> findByCardholderId(UUID cardholderId);
}
