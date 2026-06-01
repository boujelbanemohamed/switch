package com.switchplatform.platform.model.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 64, nullable = false)
    private String reference;

    @Column(name = "posting_date", nullable = false)
    private OffsetDateTime postingDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JournalStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (postingDate == null) postingDate = OffsetDateTime.now();
        if (status == null) status = JournalStatus.DRAFT;
    }

    public enum JournalStatus {
        DRAFT, POSTED, REVERSED
    }
}
