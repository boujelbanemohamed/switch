package com.switchplatform.platform.repository.ledger;

import com.switchplatform.platform.model.ledger.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    List<JournalEntry> findByStatus(JournalEntry.JournalStatus status);
}
