package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.model.clearing.InterchangeFee;
import com.switchplatform.platform.model.clearing.InterchangeResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InterchangeService {

    private final Map<String, InterchangeFee> feeConfig = new ConcurrentHashMap<>();

    public void configureFee(String brand, String cardType, String region, String mcc,
                             BigDecimal flatFee, BigDecimal percentageFee) {
        String b = brand != null ? brand.toUpperCase() : "*";
        String ct = cardType != null ? cardType.toUpperCase() : "*";
        String r = region != null ? region.toUpperCase() : "*";
        String m = mcc != null ? mcc : "*";
        String key = buildKey(b, ct, r, m);
        feeConfig.put(key, new InterchangeFee(b, ct, r, m, flatFee, percentageFee));
        log.info("Configured interchange fee: {} -> flat={}, pct={}", key, flatFee, percentageFee);
    }

    public InterchangeFee getFee(String brand, String cardType, String region, String mcc) {
        String b = brand != null ? brand.toUpperCase() : "*";
        String ct = cardType != null ? cardType.toUpperCase() : "*";
        String r = region != null ? region.toUpperCase() : "*";
        String m = mcc != null ? mcc : "*";

        InterchangeFee fee;

        fee = feeConfig.get(buildKey(b, ct, r, m));
        if (fee != null) return fee;

        fee = feeConfig.get(buildKey(b, ct, r, "*"));
        if (fee != null) return fee;

        fee = feeConfig.get(buildKey(b, ct, "*", "*"));
        if (fee != null) return fee;

        fee = feeConfig.get(buildKey(b, "*", "*", "*"));
        if (fee != null) return fee;

        return feeConfig.get(buildKey("*", "*", "*", "*"));
    }

    public InterchangeResult calculateInterchange(String brand, String cardType, String region,
                                                   String mcc, BigDecimal amount, String currencyCode) {
        InterchangeFee fee = getFee(brand, cardType, region, mcc);
        if (fee == null) {
            log.warn("No interchange fee configured for brand={}, cardType={}, region={}, mcc={}",
                      brand, cardType, region, mcc);
            return new InterchangeResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "No fee configuration found");
        }

        BigDecimal percentageAmount = amount.multiply(fee.percentageFee())
            .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
        BigDecimal totalFee = percentageAmount.add(fee.flatFee())
            .setScale(3, RoundingMode.HALF_UP);

        String currency = currencyCode != null ? currencyCode : "TND";
        String breakdown = String.format(
            "Brand=%s, Type=%s, Region=%s, MCC=%s: Flat=%s + %.4f%%%% = %s %s",
            fee.brand(), fee.cardType(), fee.region(), fee.mcc(),
            fee.flatFee(), fee.percentageFee(), totalFee, currency);

        log.info("Interchange calculated: {}", breakdown);
        return new InterchangeResult(fee.flatFee(), percentageAmount, totalFee, breakdown);
    }

    @PostConstruct
    public void initDefaultFees() {
        configureFee("VISA", "DEBIT", "TN", "*",
            new BigDecimal("0.50"), new BigDecimal("0.8"));
        configureFee("VISA", "CREDIT", "TN", "*",
            new BigDecimal("0.75"), new BigDecimal("1.0"));
        configureFee("MASTERCARD", "DEBIT", "TN", "*",
            new BigDecimal("0.45"), new BigDecimal("0.75"));
        configureFee("MASTERCARD", "CREDIT", "TN", "*",
            new BigDecimal("0.70"), new BigDecimal("0.95"));

        configureFee("VISA", "DEBIT", "INTL", "*",
            BigDecimal.ZERO, new BigDecimal("1.2"));
        configureFee("VISA", "CREDIT", "INTL", "*",
            BigDecimal.ZERO, new BigDecimal("1.5"));
        configureFee("MASTERCARD", "DEBIT", "INTL", "*",
            BigDecimal.ZERO, new BigDecimal("1.1"));
        configureFee("MASTERCARD", "CREDIT", "INTL", "*",
            BigDecimal.ZERO, new BigDecimal("1.4"));

        log.info("Default interchange fees initialized");
    }

    public Map<String, InterchangeFee> getAllFees() {
        return Map.copyOf(feeConfig);
    }

    private static String buildKey(String brand, String cardType, String region, String mcc) {
        return brand + ":" + cardType + ":" + region + ":" + mcc;
    }
}
