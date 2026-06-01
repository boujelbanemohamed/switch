package com.switchplatform.platform.service.merchant;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.model.merchant.MerchantApiKey;
import com.switchplatform.platform.model.merchant.MerchantWebhook;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantSettlementRepository;
import com.switchplatform.platform.repository.acquiring.TerminalRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantPortalService {

    private final MerchantRepository merchantRepository;
    private final TerminalRepository terminalRepository;
    private final MerchantSettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;
    private final EpgTransactionRepository epgTransactionRepository;
    private final com.switchplatform.platform.repository.merchant.MerchantWebhookRepository merchantWebhookRepository;
    private final com.switchplatform.platform.repository.merchant.MerchantApiKeyRepository merchantApiKeyRepository;

    public Merchant getMerchantByCode(String merchantCode) {
        return merchantRepository.findByMerchantId(merchantCode)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantCode));
    }

    public Map<String, Object> getDashboard(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);
        UUID merchantUuid = merchant.getId();

        long terminalCount = terminalRepository.countByMerchantId(merchantUuid);
        long activeTerminals = terminalRepository.countByMerchantIdAndStatus(merchantUuid, Terminal.TerminalStatus.ACTIVE);

        List<Transaction> switchTransactions = transactionRepository.findByMerchantId(merchantCode);
        List<EpgTransaction> epgTransactions = epgTransactionRepository.findByMerchantId(merchantUuid);

        BigDecimal totalSwitchVolume = switchTransactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEpgVolume = epgTransactions.stream()
                .filter(t -> t.getAmount() != null && t.getStatus() == EpgTransaction.Status.CAPTURED)
                .map(EpgTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalSwitchCount = switchTransactions.size();
        long totalEpgCount = epgTransactions.size();

        List<MerchantSettlement> settlements = settlementRepository.findByMerchantIdOrderBySettlementDateDesc(merchantUuid);
        BigDecimal totalSettled = settlements.stream()
                .filter(s -> s.getStatus() == MerchantSettlement.SettlementStatus.PAID
                        || s.getStatus() == MerchantSettlement.SettlementStatus.CONFIRMED)
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingSettlements = settlements.stream()
                .filter(s -> s.getStatus() == MerchantSettlement.SettlementStatus.PENDING)
                .count();

        long refundedCount = epgTransactions.stream()
                .filter(t -> t.getStatus() == EpgTransaction.Status.REFUNDED)
                .count();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("merchantCode", merchantCode);
        dashboard.put("merchantName", merchant.getTradingName());
        dashboard.put("merchantStatus", merchant.getStatus());
        dashboard.put("totalTerminals", terminalCount);
        dashboard.put("activeTerminals", activeTerminals);
        dashboard.put("totalSwitchTransactions", totalSwitchCount);
        dashboard.put("totalEpgTransactions", totalEpgCount);
        dashboard.put("totalTransactions", totalSwitchCount + totalEpgCount);
        dashboard.put("totalSwitchVolume", totalSwitchVolume);
        dashboard.put("totalEpgVolume", totalEpgVolume);
        dashboard.put("totalVolume", totalSwitchVolume.add(totalEpgVolume));
        dashboard.put("totalSettled", totalSettled);
        dashboard.put("pendingSettlements", pendingSettlements);
        dashboard.put("refundedCount", refundedCount);
        dashboard.put("currency", merchant.getSettlementCurrency());

        return dashboard;
    }

    public List<Map<String, Object>> getTransactions(String merchantCode, int page, int size) {
        Merchant merchant = getMerchantByCode(merchantCode);
        org.springframework.data.domain.Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<Map<String, Object>> result = new ArrayList<>();

        List<Transaction> switchTxns = transactionRepository
                .findByMerchantId(merchantCode, pageable).getContent();
        for (Transaction t : switchTxns) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("transactionId", t.getTransactionId());
            item.put("type", "TPE");
            item.put("protocol", t.getProtocol());
            item.put("messageType", t.getMessageType());
            item.put("amount", t.getAmount());
            item.put("currency", t.getCurrencyCode());
            item.put("stan", t.getStan());
            item.put("rrn", t.getRrn());
            item.put("status", t.getStatus());
            item.put("responseCode", t.getResponseCode());
            item.put("terminalId", t.getTerminalId());
            item.put("createdAt", t.getCreatedAt());
            result.add(item);
        }

        List<EpgTransaction> epgTxns = epgTransactionRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchant.getId(), pageable).getContent();
        for (EpgTransaction t : epgTxns) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("transactionId", t.getMerchantTransactionId());
            item.put("type", "ECOMMERCE");
            item.put("protocol", "ECOMMERCE");
            item.put("messageType", "PAYMENT");
            item.put("amount", t.getAmount());
            item.put("currency", t.getCurrencyCode());
            item.put("stan", null);
            item.put("rrn", null);
            item.put("status", t.getStatus());
            item.put("responseCode", t.getErrorCode());
            item.put("terminalId", null);
            item.put("cardholderName", t.getCardholderName());
            item.put("customerEmail", t.getCustomerEmail());
            item.put("deviceChannel", t.getDeviceChannel());
            item.put("createdAt", t.getCreatedAt());
            result.add(item);
        }

        result.sort((a, b) -> {
            OffsetDateTime da = (OffsetDateTime) a.get("createdAt");
            OffsetDateTime db = (OffsetDateTime) b.get("createdAt");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        return result;
    }

    public List<Terminal> getTerminals(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);
        return terminalRepository.findByMerchantId(merchant.getId());
    }

    public List<MerchantSettlement> getSettlements(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);
        return settlementRepository.findByMerchantIdOrderBySettlementDateDesc(merchant.getId());
    }

    public EpgTransaction refundEpgTransaction(String merchantCode, UUID epgTransactionId) {
        Merchant merchant = getMerchantByCode(merchantCode);
        EpgTransaction txn = epgTransactionRepository.findById(epgTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("EPG transaction not found: " + epgTransactionId));

        if (!txn.getMerchantId().equals(merchant.getId())) {
            throw new IllegalArgumentException("Transaction does not belong to this merchant");
        }
        if (txn.getStatus() != EpgTransaction.Status.CAPTURED
                && txn.getStatus() != EpgTransaction.Status.AUTHORIZED) {
            throw new IllegalArgumentException("Transaction cannot be refunded (status: " + txn.getStatus() + ")");
        }

        txn.setStatus(EpgTransaction.Status.REFUNDED);
        epgTransactionRepository.save(txn);
        log.info("Merchant {} refunded EPG transaction {}", merchantCode, epgTransactionId);
        return txn;
    }

    public Map<String, Object> getReportData(String merchantCode, LocalDate from, LocalDate to) {
        Merchant merchant = getMerchantByCode(merchantCode);

        OffsetDateTime fromDt = from.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime toDt   = to.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        List<Transaction> switchTxns = transactionRepository
                .findByMerchantIdAndCreatedAtBetween(merchantCode, fromDt, toDt);
        List<EpgTransaction> epgTxns = epgTransactionRepository
                .findByMerchantIdAndCreatedAtBetween(merchant.getId(), fromDt, toDt);
        List<MerchantSettlement> settlements = settlementRepository.findByMerchantIdOrderBySettlementDateDesc(merchant.getId());

        long totalTxns = switchTxns.size() + epgTxns.size();
        BigDecimal totalVolume = switchTxns.stream()
                .filter(t -> t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(epgTxns.stream()
                        .filter(t -> t.getAmount() != null)
                        .map(EpgTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        long approvedCount = switchTxns.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .count();
        long failedCount = switchTxns.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED
                        || t.getStatus() == Transaction.TransactionStatus.REJECTED)
                .count();

        BigDecimal totalFees = settlements.stream()
                .filter(s -> s.getTotalFees() != null)
                .map(MerchantSettlement::getTotalFees)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("merchantCode", merchantCode);
        report.put("merchantName", merchant.getTradingName());
        report.put("periodFrom", from);
        report.put("periodTo", to);
        report.put("totalTransactions", totalTxns);
        report.put("totalVolume", totalVolume);
        report.put("approvedCount", approvedCount);
        report.put("failedCount", failedCount);
        report.put("successRate", totalTxns > 0 ? (double) approvedCount / totalTxns * 100 : 0.0);
        report.put("totalFees", totalFees);
        report.put("totalSettlements", settlements.size());
        report.put("currency", merchant.getSettlementCurrency());

        return report;
    }

    public Map<String, Object> getInfo(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("merchantCode", merchant.getMerchantId());
        info.put("legalName", merchant.getLegalName());
        info.put("tradingName", merchant.getTradingName());
        info.put("merchantCategoryCode", merchant.getMerchantCategoryCode());
        info.put("registrationNumber", merchant.getRegistrationNumber());
        info.put("taxId", merchant.getTaxId());
        info.put("email", merchant.getEmail());
        info.put("phone", merchant.getPhone());
        info.put("website", merchant.getWebsite());
        info.put("addressLine1", merchant.getAddressLine1());
        info.put("addressLine2", merchant.getAddressLine2());
        info.put("city", merchant.getCity());
        info.put("postalCode", merchant.getPostalCode());
        info.put("countryCode", merchant.getCountryCode());
        info.put("status", merchant.getStatus());
        info.put("riskLevel", merchant.getRiskLevel());
        info.put("onboardingDate", merchant.getOnboardingDate());
        info.put("activationDate", merchant.getActivationDate());
        info.put("settlementMethod", merchant.getSettlementMethod());
        info.put("settlementCurrency", merchant.getSettlementCurrency());
        info.put("settlementAccountIban", merchant.getSettlementAccountIban());
        info.put("settlementCycle", merchant.getSettlementCycle());
        info.put("mdrPercentage", merchant.getMdrPercentage());
        info.put("mdrFixedFee", merchant.getMdrFixedFee());
        Participant ap = merchant.getAcquiringParticipant();
        info.put("acquiringParticipant", ap != null ? Map.of(
            "id", ap.getId(),
            "code", ap.getCode(),
            "name", ap.getName()
        ) : null);
        return info;
    }

    public List<Map<String, Object>> getDailyStats(String merchantCode, int days) {
        Merchant merchant = getMerchantByCode(merchantCode);
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);

        List<Transaction> txns = transactionRepository.findByMerchantId(merchantCode)
                .stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .toList();

        Map<LocalDate, List<Transaction>> byDay = txns.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    t -> t.getCreatedAt().toLocalDate()
                ));

        List<Map<String, Object>> stats = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            List<Transaction> dayTxns = byDay.getOrDefault(day, List.of());
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("date", day.toString());
            stat.put("count", dayTxns.size());
            stat.put("volume", dayTxns.stream()
                    .filter(t -> t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stat.put("approved", dayTxns.stream()
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                    .count());
            stats.add(stat);
        }
        return stats;
    }

    public List<MerchantWebhook> getWebhooks(String merchantCode) {
        return merchantWebhookRepository.findByMerchantCode(merchantCode);
    }

    public MerchantWebhook createWebhook(String merchantCode, String url, String eventTypes, String secret) {
        MerchantWebhook webhook = MerchantWebhook.builder()
                .merchantCode(merchantCode)
                .url(url)
                .eventTypes(eventTypes)
                .secret(secret)
                .enabled(true)
                .build();
        return merchantWebhookRepository.save(webhook);
    }

    public MerchantWebhook updateWebhook(UUID webhookId, String url, String eventTypes, boolean enabled) {
        MerchantWebhook webhook = merchantWebhookRepository.findById(webhookId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));
        if (url != null) webhook.setUrl(url);
        if (eventTypes != null) webhook.setEventTypes(eventTypes);
        webhook.setEnabled(enabled);
        return merchantWebhookRepository.save(webhook);
    }

    public void deleteWebhook(UUID webhookId) {
        merchantWebhookRepository.deleteById(webhookId);
    }

    public List<MerchantApiKey> getApiKeys(String merchantCode) {
        return merchantApiKeyRepository.findByMerchantCode(merchantCode);
    }

    public MerchantApiKey createApiKey(String merchantCode, String label, String permissions) {
        String apiKey = "sw_" + java.util.UUID.randomUUID().toString().replace("-", "");
        MerchantApiKey key = MerchantApiKey.builder()
                .merchantCode(merchantCode)
                .apiKey(apiKey)
                .label(label)
                .permissions(permissions)
                .enabled(true)
                .build();
        return merchantApiKeyRepository.save(key);
    }

    public MerchantApiKey revokeApiKey(UUID keyId) {
        MerchantApiKey key = merchantApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
        key.setEnabled(false);
        return merchantApiKeyRepository.save(key);
    }
}
