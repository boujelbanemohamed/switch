package com.switchplatform.platform.repository.ledger;

import com.switchplatform.platform.model.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByJournalId(UUID journalId);

    List<LedgerEntry> findByAccountId(UUID accountId);

    List<LedgerEntry> findByTransactionReference(String transactionReference);
}
