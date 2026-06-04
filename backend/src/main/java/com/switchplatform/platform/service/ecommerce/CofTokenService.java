package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.CofToken;
import com.switchplatform.platform.model.ecommerce.RecurringSchedule;
import com.switchplatform.platform.repository.ecommerce.CofTokenRepository;
import com.switchplatform.platform.repository.ecommerce.RecurringScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CofTokenService {

    private final CofTokenRepository cofTokenRepository;
    private final RecurringScheduleRepository recurringScheduleRepository;

    public CofToken createToken(CofToken token) {
        return cofTokenRepository.save(token);
    }

    public CofToken getToken(UUID id) {
        return cofTokenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CofToken not found: " + id));
    }

    public List<CofToken> listTokens(UUID participantId) {
        if (participantId != null) {
            return cofTokenRepository.findByParticipantId(participantId);
        }
        return cofTokenRepository.findAll();
    }

    public CofToken updateToken(UUID id, CofToken update) {
        CofToken existing = getToken(id);
        if (update.getPanDisplay() != null) existing.setPanDisplay(update.getPanDisplay());
        if (update.getExpiryMonth() != null) existing.setExpiryMonth(update.getExpiryMonth());
        if (update.getExpiryYear() != null) existing.setExpiryYear(update.getExpiryYear());
        if (update.getCardholderName() != null) existing.setCardholderName(update.getCardholderName());
        if (update.getTokenType() != null) existing.setTokenType(update.getTokenType());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        return cofTokenRepository.save(existing);
    }

    public void deleteToken(UUID id) {
        cofTokenRepository.deleteById(id);
    }

    @Transactional
    public RecurringSchedule createSchedule(RecurringSchedule schedule) {
        cofTokenRepository.findById(schedule.getCofTokenId())
                .orElseThrow(() -> new EntityNotFoundException("CofToken not found: " + schedule.getCofTokenId()));
        return recurringScheduleRepository.save(schedule);
    }

    public RecurringSchedule getSchedule(UUID id) {
        return recurringScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RecurringSchedule not found: " + id));
    }

    public List<RecurringSchedule> listSchedules(UUID cofTokenId) {
        if (cofTokenId != null) {
            return recurringScheduleRepository.findByCofTokenId(cofTokenId);
        }
        return recurringScheduleRepository.findAll();
    }

    public RecurringSchedule updateSchedule(UUID id, RecurringSchedule update) {
        RecurringSchedule existing = getSchedule(id);
        if (update.getAmount() != null) existing.setAmount(update.getAmount());
        if (update.getCurrencyCode() != null) existing.setCurrencyCode(update.getCurrencyCode());
        if (update.getFrequency() != null) existing.setFrequency(update.getFrequency());
        if (update.getNextRunDate() != null) existing.setNextRunDate(update.getNextRunDate());
        if (update.getEndDate() != null) existing.setEndDate(update.getEndDate());
        if (update.getMaxOccurrences() != null) existing.setMaxOccurrences(update.getMaxOccurrences());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        return recurringScheduleRepository.save(existing);
    }

    public void deleteSchedule(UUID id) {
        recurringScheduleRepository.deleteById(id);
    }

    public void processDueSchedules() {
        List<RecurringSchedule> due = recurringScheduleRepository
                .findByNextRunDateLessThanEqualAndStatus(LocalDate.now(), "ACTIVE");
        for (RecurringSchedule schedule : due) {
            try {
                log.info("Processing recurring schedule: {} for amount {}", schedule.getId(), schedule.getAmount());
                schedule.setOccurrencesProcessed(schedule.getOccurrencesProcessed() + 1);
                if (schedule.getNextRunDate() != null) {
                    schedule.setNextRunDate(schedule.getNextRunDate().plusDays(switch (schedule.getFrequency()) {
                        case "DAILY" -> 1;
                        case "WEEKLY" -> 7;
                        case "MONTHLY" -> 30;
                        default -> 30;
                    }));
                }
                if (schedule.getMaxOccurrences() != null
                        && schedule.getOccurrencesProcessed() >= schedule.getMaxOccurrences()) {
                    schedule.setStatus("COMPLETED");
                }
                if (schedule.getEndDate() != null && LocalDate.now().isAfter(schedule.getEndDate())) {
                    schedule.setStatus("COMPLETED");
                }
                recurringScheduleRepository.save(schedule);
            } catch (Exception e) {
                log.error("Failed to process schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }
}
