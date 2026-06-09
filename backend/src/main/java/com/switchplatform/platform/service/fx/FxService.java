package com.switchplatform.platform.service.fx;

import com.switchplatform.platform.model.fx.FxRate;
import com.switchplatform.platform.repository.fx.FxRateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FxService {

    private final FxRateRepository fxRateRepository;

    public FxRate createRate(FxRate rate) {
        return fxRateRepository.save(rate);
    }

    public FxRate getRate(UUID id) {
        return fxRateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FxRate not found: " + id));
    }

    public List<FxRate> listRates() {
        return fxRateRepository.findAll();
    }

    public FxRate updateRate(UUID id, FxRate update) {
        FxRate existing = getRate(id);
        if (update.getRate() != null) existing.setRate(update.getRate());
        if (update.getMarginPercentage() != null) existing.setMarginPercentage(update.getMarginPercentage());
        if (update.getEffectiveDate() != null) existing.setEffectiveDate(update.getEffectiveDate());
        if (update.getExpiryDate() != null) existing.setExpiryDate(update.getExpiryDate());
        if (update.getSource() != null) existing.setSource(update.getSource());
        return fxRateRepository.save(existing);
    }

    public void deleteRate(UUID id) {
        fxRateRepository.deleteById(id);
    }

    public BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            return amount;
        }
        FxRate rate = fxRateRepository
                .findTopBySourceCurrencyAndTargetCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        sourceCurrency.toUpperCase(), targetCurrency.toUpperCase(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No rate found for " + sourceCurrency + "/" + targetCurrency));
        return amount.multiply(rate.getRate()).setScale(3, RoundingMode.HALF_UP);
    }

    public BigDecimal proposeDcc(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        BigDecimal converted = convert(amount, sourceCurrency, targetCurrency);
        FxRate rate = fxRateRepository
                .findTopBySourceCurrencyAndTargetCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        sourceCurrency.toUpperCase(), targetCurrency.toUpperCase(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("No rate found"));
        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                rate.getMarginPercentage().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return converted.multiply(marginMultiplier).setScale(3, RoundingMode.HALF_UP);
    }
}
