package com.switchplatform.platform.repository.ledger;

import com.switchplatform.platform.model.ledger.AccountingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountingTransactionRepository extends JpaRepository<AccountingTransaction, UUID> {

    Optional<AccountingTransaction> findByReference(String reference);

    Optional<AccountingTransaction> findByJournalId(UUID journalId);
}
