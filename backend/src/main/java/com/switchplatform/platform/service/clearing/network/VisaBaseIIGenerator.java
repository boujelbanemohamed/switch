package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class VisaBaseIIGenerator implements NetworkClearingGenerator {

    @Override
    public String scheme() {
        return "VISA";
    }

    @Override
    public String generate(LocalDate date, List<ClearingRecord> records) {
        throw new UnsupportedOperationException(
                "Visa BASE II format layout pending official Visa specification. " +
                "This generator cannot produce valid Visa clearing files without " +
                "the proprietary BASE II field positions and record types.");
    }

    @Override
    public ReconciliationResult ingest(String content) {
        throw new UnsupportedOperationException(
                "Visa BASE II ingestion pending official Visa specification.");
    }
}
