package com.switchplatform.platform.service.merchant;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantSettlementRepository;
import com.switchplatform.platform.repository.acquiring.TerminalRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<Map<String, Object>> getTransactions(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);

        List<Map<String, Object>> result = new ArrayList<>();

        List<Transaction> switchTxns = transactionRepository.findByMerchantId(merchantCode);
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

        List<EpgTransaction> epgTxns = epgTransactionRepository.findByMerchantId(merchant.getId());
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

        List<Transaction> switchTxns = transactionRepository.findByMerchantId(merchantCode);
        List<EpgTransaction> epgTxns = epgTransactionRepository.findByMerchantId(merchant.getId());
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
}
