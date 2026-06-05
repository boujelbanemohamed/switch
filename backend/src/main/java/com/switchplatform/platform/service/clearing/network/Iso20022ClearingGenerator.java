package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class Iso20022ClearingGenerator implements NetworkClearingGenerator {

    private final Iso20022Engine iso20022Engine;

    @Override
    public String scheme() {
        return "ISO20022";
    }

    @Override
    public String generate(LocalDate date, List<ClearingRecord> records) {
        BigDecimal totalAmount = records.stream()
                .map(ClearingRecord::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String msgId = "SCM-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String currency = records.isEmpty() ? "TND" : records.get(0).getCurrencyCode();

        Document doc = iso20022Engine.createPaymentRequest(
                msgId, "SWITCH", "SWITCH", totalAmount, currency,
                "SWITCH-CLR", "SWITCH-CLR", "SCHEME-CLEARING-" + date);
        return iso20022Engine.toXml(doc);
    }

    @Override
    public ReconciliationResult ingest(String content) {
        try {
            Document doc = iso20022Engine.parse(content);
            var details = iso20022Engine.extractPaymentDetails(doc);
            log.info("ISO 20022 clearing ingested: msgId={}, amount={}",
                    details.get("MsgId"), details.get("IntrBkSttlmAmt"));
            return new ReconciliationResult(1, 0, 1);
        } catch (Exception e) {
            log.warn("ISO 20022 ingestion parse failed: {}", e.getMessage());
            return new ReconciliationResult(0, 0, 0);
        }
    }
}
