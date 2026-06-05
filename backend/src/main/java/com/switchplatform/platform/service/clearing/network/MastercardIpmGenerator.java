package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class MastercardIpmGenerator implements NetworkClearingGenerator {

    @Override
    public String scheme() {
        return "MASTERCARD";
    }

    @Override
    public String generate(LocalDate date, List<ClearingRecord> records) {
        throw new UnsupportedOperationException(
                "Mastercard IPM format layout pending official Mastercard specification. " +
                "This generator cannot produce valid Mastercard clearing files without " +
                "the proprietary IPM record layout and field definitions.");
    }

    @Override
    public ReconciliationResult ingest(String content) {
        throw new UnsupportedOperationException(
                "Mastercard IPM ingestion pending official Mastercard specification.");
    }
}
