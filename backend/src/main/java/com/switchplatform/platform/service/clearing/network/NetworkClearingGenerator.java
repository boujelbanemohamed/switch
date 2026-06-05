package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.clearing.ClearingRecord;

import java.time.LocalDate;
import java.util.List;

public interface NetworkClearingGenerator {
    String scheme();
    String generate(LocalDate date, List<ClearingRecord> records);
    ReconciliationResult ingest(String content);

    record ReconciliationResult(int totalRecords, int matched, int unmatched) {}
}
