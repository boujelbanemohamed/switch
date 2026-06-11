package com.switchplatform.platform.service.simulator;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.MultilateralNettingSession;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.clearing.ReconciliationRecordRepository;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.clearing.MultilateralNettingService;
import com.switchplatform.platform.service.clearing.SettlementFileService;
import com.switchplatform.platform.service.clearing.bct.BctSettlementService;
import com.switchplatform.platform.service.clearing.network.Iso20022ClearingGenerator;
import com.switchplatform.platform.service.clearing.network.MastercardIpmGenerator;
import com.switchplatform.platform.service.clearing.network.VisaBaseIIGenerator;
import com.switchplatform.platform.service.clearing.network.mastercard.MastercardIpmSimConfig;
import com.switchplatform.platform.service.clearing.network.visa.VisaBaseIISimConfig;
import com.switchplatform.platform.service.routing.BinTableService;
import lombok.AllArgsConstructor;

import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClearingCycleService {

    private final ClearingRecordRepository clearingRecordRepository;
    private final Iso20022ClearingGenerator iso20022Generator;
    private final VisaBaseIIGenerator visaBaseIIGenerator;
    private final MastercardIpmGenerator mastercardIpmGenerator;
    private final SettlementFileService settlementFileService;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final ParticipantRepository participantRepository;
    private final BinTableService binTableService;
    private final MultilateralNettingService multilateralNettingService;
    private final BctSettlementService bctSettlementService;
    private final ClearingService clearingService;

    @Transactional
    public ClearingCycleResult executeFullCycle() {
        List<ClearingRecord> pendingRecords = clearingRecordRepository.findByStatus(ClearingRecord.Status.PENDING);
        if (pendingRecords.isEmpty()) {
            return ClearingCycleResult.builder()
                    .sentCount(0).matchedCount(0).discrepancyCount(0).unmatchedCount(0)
                    .generatedFile("").networkResponseFile("")
                    .message("No PENDING clearing records to process")
                    .build();
        }

        LocalDate today = LocalDate.now();

        String iso20022Xml = iso20022Generator.generate(today, pendingRecords);

        SimNetworkResponse simulated = buildNetworkResponse(pendingRecords);
        String csvResponse = simulated.csvContent;

        Participant acquirer = participantRepository.findByCode("BANK_A")
                .orElse(null);

        if (acquirer != null) {
            settlementFileService.ingestIncomingClearingFile(csvResponse, "CSV", acquirer.getId());
        }

        List<RecordResult> details = new ArrayList<>();
        int matched = 0;
        int discrepancy = 0;
        int unmatched = 0;

        for (ClearingRecord record : pendingRecords) {
            String txId = record.getTransactionId();
            BigDecimal sentAmount = record.getAmount();
            BigDecimal responseAmount = simulated.responseAmounts.get(txId);

            if (responseAmount == null) {
                unmatched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(null)
                        .outcome("UNMATCHED").reason("Not present in network response")
                        .build());
            } else if (responseAmount.compareTo(sentAmount) != 0) {
                discrepancy++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("DISCREPANCY")
                        .reason("Amount mismatch: sent " + sentAmount + " vs received " + responseAmount
                                + " (diff=" + sentAmount.subtract(responseAmount) + ")")
                        .build());
            } else {
                matched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("RECONCILED").reason("")
                        .build());
            }
        }

        UUID reconId = null;
        try {
            ReconciliationRecord.Status status = (discrepancy > 0 || unmatched > 0)
                    ? ReconciliationRecord.Status.PARTIALLY_MATCHED
                    : ReconciliationRecord.Status.MATCHED;

            ReconciliationRecord recon = ReconciliationRecord.builder()
                    .reconciliationDate(today)
                    .participantId(acquirer != null ? acquirer.getId() : null)
                    .source(ReconciliationRecord.Source.SCHEME)
                    .totalTransactions(pendingRecords.size())
                    .totalAmount(pendingRecords.stream()
                            .map(ClearingRecord::getAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .matchedCount(matched)
                    .unmatchedCount(unmatched)
                    .discrepancyCount(discrepancy)
                    .status(status)
                    .notes("ISO20022 cycle: " + matched + " matched, " + discrepancy + " discrepancies, "
                            + unmatched + " unmatched")
                    .build();
            recon = reconciliationRecordRepository.save(recon);
            reconId = recon.getId();
        } catch (Exception e) {
            log.warn("Failed to persist reconciliation record: {}", e.getMessage());
        }

        log.info("Clearing cycle complete: sent={}, matched={}, discrepancy={}, unmatched={}",
                pendingRecords.size(), matched, discrepancy, unmatched);

        return ClearingCycleResult.builder()
                .format("ISO20022")
                .sentCount(pendingRecords.size())
                .matchedCount(matched)
                .discrepancyCount(discrepancy)
                .unmatchedCount(unmatched)
                .generatedFile(iso20022Xml)
                .networkResponseFile(csvResponse)
                .details(details)
                .reconciliationRecordId(reconId)
                .build();
    }

    @Transactional
    public ClearingCycleResult executeVisaBaseIiCycle() {
        List<ClearingRecord> pendingRecords = clearingRecordRepository.findByStatus(ClearingRecord.Status.PENDING);
        if (pendingRecords.isEmpty()) {
            return ClearingCycleResult.builder()
                    .format("VISA_BASE_II")
                    .sentCount(0).matchedCount(0).discrepancyCount(0).unmatchedCount(0)
                    .generatedFile("").networkResponseFile("")
                    .message("No PENDING clearing records to process")
                    .build();
        }

        LocalDate today = LocalDate.now();

        String visaBaseII = visaBaseIIGenerator.generate(today, pendingRecords);

        SimNetworkResponse simulated = buildNetworkResponse(pendingRecords);
        String csvResponse = simulated.csvContent;

        Participant acquirer = participantRepository.findByCode("BANK_A")
                .orElse(null);

        if (acquirer != null) {
            settlementFileService.ingestIncomingClearingFile(csvResponse, "VISA", acquirer.getId());
        }

        List<RecordResult> details = new ArrayList<>();
        int matched = 0;
        int discrepancy = 0;
        int unmatched = 0;

        for (ClearingRecord record : pendingRecords) {
            String txId = record.getTransactionId();
            BigDecimal sentAmount = record.getAmount();
            BigDecimal responseAmount = simulated.responseAmounts.get(txId);

            if (responseAmount == null) {
                unmatched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(null)
                        .outcome("UNMATCHED").reason("Not present in Visa BASE II network response")
                        .build());
            } else if (responseAmount.compareTo(sentAmount) != 0) {
                discrepancy++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("DISCREPANCY")
                        .reason("Amount mismatch: sent " + sentAmount + " vs received " + responseAmount
                                + " (diff=" + sentAmount.subtract(responseAmount) + ")")
                        .build());
            } else {
                matched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("RECONCILED").reason("")
                        .build());
            }
        }

        UUID reconId = null;
        try {
            ReconciliationRecord.Status status = (discrepancy > 0 || unmatched > 0)
                    ? ReconciliationRecord.Status.PARTIALLY_MATCHED
                    : ReconciliationRecord.Status.MATCHED;

            ReconciliationRecord recon = ReconciliationRecord.builder()
                    .reconciliationDate(today)
                    .participantId(acquirer != null ? acquirer.getId() : null)
                    .source(ReconciliationRecord.Source.SCHEME)
                    .totalTransactions(pendingRecords.size())
                    .totalAmount(pendingRecords.stream()
                            .map(ClearingRecord::getAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .matchedCount(matched)
                    .unmatchedCount(unmatched)
                    .discrepancyCount(discrepancy)
                    .status(status)
                    .notes("Visa BASE II cycle: " + matched + " matched, " + discrepancy + " discrepancies, "
                            + unmatched + " unmatched")
                    .build();
            recon = reconciliationRecordRepository.save(recon);
            reconId = recon.getId();
        } catch (Exception e) {
            log.warn("Failed to persist reconciliation record: {}", e.getMessage());
        }

        log.info("Visa BASE II cycle complete: sent={}, matched={}, discrepancy={}, unmatched={}",
                pendingRecords.size(), matched, discrepancy, unmatched);

        return ClearingCycleResult.builder()
                .format("VISA_BASE_II")
                .sentCount(pendingRecords.size())
                .matchedCount(matched)
                .discrepancyCount(discrepancy)
                .unmatchedCount(unmatched)
                .generatedFile(visaBaseII)
                .networkResponseFile(csvResponse)
                .details(details)
                .reconciliationRecordId(reconId)
                .build();
    }

    @Transactional
    public ClearingCycleResult executeMastercardIpmCycle() {
        List<ClearingRecord> pendingRecords = clearingRecordRepository.findByStatus(ClearingRecord.Status.PENDING);
        if (pendingRecords.isEmpty()) {
            return ClearingCycleResult.builder()
                    .format("MASTERCARD_IPM")
                    .sentCount(0).matchedCount(0).discrepancyCount(0).unmatchedCount(0)
                    .generatedFile("").networkResponseFile("")
                    .message("No PENDING clearing records to process")
                    .build();
        }

        LocalDate today = LocalDate.now();

        String mastercardIpm = mastercardIpmGenerator.generate(today, pendingRecords);

        SimNetworkResponse simulated = buildNetworkResponse(pendingRecords);
        String csvResponse = simulated.csvContent;

        Participant acquirer = participantRepository.findByCode("BANK_A")
                .orElse(null);

        if (acquirer != null) {
            settlementFileService.ingestIncomingClearingFile(csvResponse, "MASTERCARD", acquirer.getId());
        }

        List<RecordResult> details = new ArrayList<>();
        int matched = 0;
        int discrepancy = 0;
        int unmatched = 0;

        for (ClearingRecord record : pendingRecords) {
            String txId = record.getTransactionId();
            BigDecimal sentAmount = record.getAmount();
            BigDecimal responseAmount = simulated.responseAmounts.get(txId);

            if (responseAmount == null) {
                unmatched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(null)
                        .outcome("UNMATCHED").reason("Not present in Mastercard IPM network response")
                        .build());
            } else if (responseAmount.compareTo(sentAmount) != 0) {
                discrepancy++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("DISCREPANCY")
                        .reason("Amount mismatch: sent " + sentAmount + " vs received " + responseAmount
                                + " (diff=" + sentAmount.subtract(responseAmount) + ")")
                        .build());
            } else {
                matched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sentAmount).responseAmount(responseAmount)
                        .outcome("RECONCILED").reason("")
                        .build());
            }
        }

        UUID reconId = null;
        try {
            ReconciliationRecord.Status status = (discrepancy > 0 || unmatched > 0)
                    ? ReconciliationRecord.Status.PARTIALLY_MATCHED
                    : ReconciliationRecord.Status.MATCHED;

            ReconciliationRecord recon = ReconciliationRecord.builder()
                    .reconciliationDate(today)
                    .participantId(acquirer != null ? acquirer.getId() : null)
                    .source(ReconciliationRecord.Source.SCHEME)
                    .totalTransactions(pendingRecords.size())
                    .totalAmount(pendingRecords.stream()
                            .map(ClearingRecord::getAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .matchedCount(matched)
                    .unmatchedCount(unmatched)
                    .discrepancyCount(discrepancy)
                    .status(status)
                    .notes("Mastercard IPM cycle: " + matched + " matched, " + discrepancy + " discrepancies, "
                            + unmatched + " unmatched")
                    .build();
            recon = reconciliationRecordRepository.save(recon);
            reconId = recon.getId();
        } catch (Exception e) {
            log.warn("Failed to persist reconciliation record: {}", e.getMessage());
        }

        log.info("Mastercard IPM cycle complete: sent={}, matched={}, discrepancy={}, unmatched={}",
                pendingRecords.size(), matched, discrepancy, unmatched);

        return ClearingCycleResult.builder()
                .format("MASTERCARD_IPM")
                .sentCount(pendingRecords.size())
                .matchedCount(matched)
                .discrepancyCount(discrepancy)
                .unmatchedCount(unmatched)
                .generatedFile(mastercardIpm)
                .networkResponseFile(csvResponse)
                .details(details)
                .reconciliationRecordId(reconId)
                .build();
    }

    private SimNetworkResponse buildNetworkResponse(List<ClearingRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("clearing_date,transaction_id,acquiring_participant,issuing_participant,")
          .append("amount,currency,interchange_amount,fee_amount,net_amount,message_type,status\n");

        Map<String, BigDecimal> responseAmounts = new LinkedHashMap<>();
        Set<String> missingTxIds = new HashSet<>();

        List<ClearingRecord> toProcess = new ArrayList<>(records);
        if (toProcess.size() > 2) {
            ClearingRecord dropped = toProcess.remove(toProcess.size() - 1);
            missingTxIds.add(dropped.getTransactionId());
            log.info("Simulated network: excluding record txId={} to create UNMATCHED", dropped.getTransactionId());
        }

        for (int i = 0; i < toProcess.size(); i++) {
            ClearingRecord r = toProcess.get(i);
            BigDecimal amount = r.getAmount();
            boolean isDiscrepancy = (i == 0 && records.size() > 1);

            if (isDiscrepancy) {
                amount = amount.add(BigDecimal.TEN);
                log.info("Simulated network: altering amount for txId={}: {} → {} (injecting DISCREPANCY)",
                        r.getTransactionId(), r.getAmount(), amount);
            }

            responseAmounts.put(r.getTransactionId(), amount);

            sb.append(LocalDate.now()).append(",")
              .append(r.getTransactionId()).append(",")
              .append(r.getAcquiringParticipantId()).append(",")
              .append(r.getIssuingParticipantId()).append(",")
              .append(amount).append(",")
              .append(r.getCurrencyCode() != null ? r.getCurrencyCode() : "TND").append(",")
              .append(r.getInterchangeAmount() != null ? r.getInterchangeAmount() : "0").append(",")
              .append(r.getFeeAmount() != null ? r.getFeeAmount() : "0").append(",")
              .append(amount).append(",")
              .append(r.getMessageType() != null ? r.getMessageType() : "0100").append(",")
              .append("SETTLED\n");
        }

        return new SimNetworkResponse(sb.toString(), responseAmounts, missingTxIds);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ClearingCycleResult {
        private String format;
        private int sentCount;
        private int matchedCount;
        private int discrepancyCount;
        private int unmatchedCount;
        private String generatedFile;
        private String networkResponseFile;
        private List<RecordResult> details;
        private UUID reconciliationRecordId;
        private String message;
    }

    @Data
    @Builder
    public static class RecordResult {
        private String transactionId;
        private BigDecimal sentAmount;
        private BigDecimal responseAmount;
        private String outcome;
        private String reason;
    }

    private record SimNetworkResponse(String csvContent, Map<String, BigDecimal> responseAmounts, Set<String> missingTxIds) {}

    // ---------------------------------------------------------------
    // Phase 5 — Full unified network cycle + netting + BCT settlement
    // ---------------------------------------------------------------

    @Transactional
    public FullCycleResult executeFullNetworkCycle() {
        List<ClearingRecord> pending = clearingRecordRepository.findByStatus(ClearingRecord.Status.PENDING);
        if (pending.isEmpty()) {
            return FullCycleResult.builder()
                    .message("No PENDING clearing records to process")
                    .totalSent(0).totalMatched(0).totalDiscrepancy(0).totalUnmatched(0)
                    .build();
        }

        LocalDate today = LocalDate.now();

        Map<String, List<ClearingRecord>> byBrand = new LinkedHashMap<>();
        byBrand.put("CB", new ArrayList<>());
        byBrand.put("VISA", new ArrayList<>());
        byBrand.put("MASTERCARD", new ArrayList<>());

        for (ClearingRecord r : pending) {
            String brand = resolveCardBrand(r);
            byBrand.computeIfAbsent(brand, k -> new ArrayList<>()).add(r);
        }

        log.info("Full cycle: {} pending records distributed: CB={} VISA={} MASTERCARD={} other={}",
                pending.size(),
                byBrand.getOrDefault("CB", List.of()).size(),
                byBrand.getOrDefault("VISA", List.of()).size(),
                byBrand.getOrDefault("MASTERCARD", List.of()).size(),
                byBrand.entrySet().stream()
                        .filter(e -> !List.of("CB", "VISA", "MASTERCARD").contains(e.getKey()))
                        .mapToInt(e -> e.getValue().size()).sum());

        List<NetworkResult> networkResults = new ArrayList<>();
        int totalSent = 0, totalMatched = 0, totalDisc = 0, totalUnmatched = 0;

        for (var entry : byBrand.entrySet()) {
            String brand = entry.getKey();
            List<ClearingRecord> records = entry.getValue();
            if (records.isEmpty()) continue;

            NetworkResult nr = runNetworkCycle(brand, records, today);
            networkResults.add(nr);
            totalSent += nr.sentCount;
            totalMatched += nr.matchedCount;
            totalDisc += nr.discrepancyCount;
            totalUnmatched += nr.unmatchedCount;
        }

        for (ClearingRecord r : pending) {
            if (r.getStatus() == ClearingRecord.Status.PENDING) {
                r.setStatus(ClearingRecord.Status.CLEARED);
                clearingRecordRepository.save(r);
            }
        }

        MultilateralNettingSession netting = null;
        String bctFile = "";
        try {
            clearingService.calculateNetting(today);
            netting = multilateralNettingService.calculateNetting(today, "TND");
            bctFile = bctSettlementService.generateBctSettlementFile(today);
            log.info("Full cycle netting: session={} efficiency={}%",
                    netting.getId(), netting.getNettingEfficiency());
        } catch (Exception e) {
            log.warn("Full cycle netting/BCT failed: {}", e.getMessage());
        }

        return FullCycleResult.builder()
                .message("Full clearing cycle completed with " + networkResults.size() + " networks")
                .totalSent(totalSent).totalMatched(totalMatched)
                .totalDiscrepancy(totalDisc).totalUnmatched(totalUnmatched)
                .networkResults(networkResults)
                .nettingSessionId(netting != null ? netting.getId() : null)
                .nettingEfficiency(netting != null ? netting.getNettingEfficiency() : null)
                .nettingTotalGross(netting != null ? netting.getTotalGrossAmount() : null)
                .nettingTotalNet(netting != null ? netting.getTotalNetAmount() : null)
                .bctSettlementFile(escapeJsonControlChars(bctFile))
                .build();
    }

    private static String escapeJsonControlChars(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String resolveCardBrand(ClearingRecord record) {
        if (record.getCardBrand() != null && !record.getCardBrand().isBlank()) {
            String b = record.getCardBrand().toUpperCase();
            if (List.of("VISA", "MASTERCARD", "CB").contains(b)) return b;
            log.warn("resolveCardBrand: marque '{}' non reconnue pour record {}, routage par défaut vers CB/domestique",
                    b, record.getTransactionId());
            return "CB";
        }
        if (record.getCardNumber() != null && record.getCardNumber().length() >= 6) {
            String resolved = binTableService.resolveCardBrand(record.getCardNumber());
            if ("VISA".equals(resolved)) return "VISA";
            if ("MASTERCARD".equals(resolved)) return "MASTERCARD";
        }
        log.warn("resolveCardBrand: marque non résolue pour record {} (cardBrand vide, PAN/BIN non trouvé), routage par défaut vers CB/domestique",
                record.getTransactionId());
        return "CB";
    }

    private NetworkResult runNetworkCycle(String brand, List<ClearingRecord> records, LocalDate today) {
        String networkFile;
        String networkName;

        switch (brand) {
            case "VISA" -> {
                networkFile = visaBaseIIGenerator.generate(today, records);
                networkName = "VISA_BASE_II";
            }
            case "MASTERCARD" -> {
                networkFile = mastercardIpmGenerator.generate(today, records);
                networkName = "MASTERCARD_IPM";
            }
            default -> {
                networkFile = iso20022Generator.generate(today, records);
                networkName = "ISO20022";
            }
        }

        SimNetworkResponse simulated = buildNetworkResponse(records);
        String responseFile = simulated.csvContent;

        List<RecordResult> details = new ArrayList<>();
        int matched = 0, discrepancy = 0, unmatched = 0;

        for (ClearingRecord record : records) {
            String txId = record.getTransactionId();
            BigDecimal sent = record.getAmount();
            BigDecimal rsp = simulated.responseAmounts.get(txId);

            if (rsp == null) {
                unmatched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sent).responseAmount(null)
                        .outcome("UNMATCHED").reason("Not in " + networkName + " response")
                        .build());
            } else if (rsp.compareTo(sent) != 0) {
                discrepancy++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sent).responseAmount(rsp)
                        .outcome("DISCREPANCY")
                        .reason("Diff=" + sent.subtract(rsp))
                        .build());
            } else {
                matched++;
                details.add(RecordResult.builder()
                        .transactionId(txId).sentAmount(sent).responseAmount(rsp)
                        .outcome("RECONCILED").reason("")
                        .build());
            }
        }

        return NetworkResult.builder()
                .network(networkName)
                .sentCount(records.size()).matchedCount(matched)
                .discrepancyCount(discrepancy).unmatchedCount(unmatched)
                .generatedFile(escapeJsonControlChars(networkFile))
                .networkResponseFile(escapeJsonControlChars(responseFile))
                .details(details)
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class FullCycleResult {
        private String message;
        private int totalSent;
        private int totalMatched;
        private int totalDiscrepancy;
        private int totalUnmatched;
        private List<NetworkResult> networkResults;
        private UUID nettingSessionId;
        private BigDecimal nettingEfficiency;
        private BigDecimal nettingTotalGross;
        private BigDecimal nettingTotalNet;
        private String bctSettlementFile;
    }

    @Data
    @Builder
    public static class NetworkResult {
        private String network;
        private int sentCount;
        private int matchedCount;
        private int discrepancyCount;
        private int unmatchedCount;
        private String generatedFile;
        private String networkResponseFile;
        private List<RecordResult> details;
    }
}
