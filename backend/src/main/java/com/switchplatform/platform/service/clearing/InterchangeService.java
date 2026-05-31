package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.model.clearing.InterchangeFee;
import com.switchplatform.platform.model.clearing.InterchangeResult;
import com.switchplatform.platform.repository.clearing.InterchangeFeeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterchangeService {

    private final InterchangeFeeRepository interchangeFeeRepository;

    @Transactional
    public void configureFee(String brand, String cardType, String region, String mcc,
                             BigDecimal flatFee, BigDecimal percentageFee) {
        String b = brand != null ? brand.toUpperCase() : "*";
        String ct = cardType != null ? cardType.toUpperCase() : "*";
        String r = region != null ? region.toUpperCase() : "*";
        String m = mcc != null ? mcc : "*";
        InterchangeFee fee = InterchangeFee.builder()
                .brand(b)
                .cardType(ct)
                .region(r)
                .mcc(m)
                .flatFee(flatFee)
                .percentageFee(percentageFee)
                .build();
        interchangeFeeRepository.save(fee);
        log.info("Configured interchange fee: {}:{}:{}:{} -> flat={}, pct={}", b, ct, r, m, flatFee, percentageFee);
    }

    @Transactional(readOnly = true)
    public InterchangeFee getFee(String brand, String cardType, String region, String mcc) {
        String b = brand != null ? brand.toUpperCase() : "*";
        String ct = cardType != null ? cardType.toUpperCase() : "*";
        String r = region != null ? region.toUpperCase() : "*";
        String m = mcc != null ? mcc : "*";

        InterchangeFee fee;

        fee = interchangeFeeRepository.findByBrandAndCardTypeAndRegionAndMcc(b, ct, r, m).orElse(null);
        if (fee != null) return fee;

        fee = interchangeFeeRepository.findByBrandAndCardTypeAndRegionAndMcc(b, ct, r, "*").orElse(null);
        if (fee != null) return fee;

        fee = interchangeFeeRepository.findByBrandAndCardTypeAndRegionAndMcc(b, ct, "*", "*").orElse(null);
        if (fee != null) return fee;

        fee = interchangeFeeRepository.findByBrandAndCardTypeAndRegionAndMcc(b, "*", "*", "*").orElse(null);
        if (fee != null) return fee;

        return interchangeFeeRepository.findByBrandAndCardTypeAndRegionAndMcc("*", "*", "*", "*").orElse(null);
    }

    @Transactional(readOnly = true)
    public InterchangeResult calculateInterchange(String brand, String cardType, String region,
                                                   String mcc, BigDecimal amount, String currencyCode) {
        InterchangeFee fee = getFee(brand, cardType, region, mcc);
        if (fee == null) {
            log.warn("No interchange fee configured for brand={}, cardType={}, region={}, mcc={}",
                      brand, cardType, region, mcc);
            return new InterchangeResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "No fee configuration found");
        }

        BigDecimal percentageAmount = amount.multiply(fee.getPercentageFee())
            .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
        BigDecimal totalFee = percentageAmount.add(fee.getFlatFee())
            .setScale(3, RoundingMode.HALF_UP);

        String currency = currencyCode != null ? currencyCode : "TND";
        String breakdown = String.format(
            "Brand=%s, Type=%s, Region=%s, MCC=%s: Flat=%s + %.4f%%%% = %s %s",
            fee.getBrand(), fee.getCardType(), fee.getRegion(), fee.getMcc(),
            fee.getFlatFee(), fee.getPercentageFee(), totalFee, currency);

        log.info("Interchange calculated: {}", breakdown);
        return new InterchangeResult(fee.getFlatFee(), percentageAmount, totalFee, breakdown);
    }

    @PostConstruct
    @Transactional
    public void initDefaultFees() {
        if (interchangeFeeRepository.count() > 0) {
            log.info("Interchange fees already seeded, skipping");
            return;
        }
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

    @Transactional(readOnly = true)
    public Map<String, InterchangeFee> getAllFees() {
        return interchangeFeeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        f -> buildKey(f.getBrand(), f.getCardType(), f.getRegion(), f.getMcc()),
                        Function.identity()
                ));
    }

    private static String buildKey(String brand, String cardType, String region, String mcc) {
        return brand + ":" + cardType + ":" + region + ":" + mcc;
    }
}
