package com.switchplatform.platform.service.routing;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.repository.BinTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinTableService {

    private final BinTableRepository binTableRepository;

    public List<BinTable> findAll() {
        return binTableRepository.findAll();
    }

    public BinTable findById(UUID id) {
        return binTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("BIN table not found: " + id));
    }

    public BinTable create(BinTable binTable) {
        return binTableRepository.save(binTable);
    }

    public BinTable update(UUID id, BinTable binTable) {
        BinTable existing = findById(id);
        if (binTable.getBin() != null) existing.setBin(binTable.getBin());
        if (binTable.getBinLength() != null) existing.setBinLength(binTable.getBinLength());
        if (binTable.getCardBrand() != null) existing.setCardBrand(binTable.getCardBrand());
        if (binTable.getCardType() != null) existing.setCardType(binTable.getCardType());
        if (binTable.getCountryCode() != null) existing.setCountryCode(binTable.getCountryCode());
        if (binTable.getCurrencyCode() != null) existing.setCurrencyCode(binTable.getCurrencyCode());
        if (binTable.getParticipant() != null) existing.setParticipant(binTable.getParticipant());
        if (binTable.getIsActive() != null) existing.setIsActive(binTable.getIsActive());
        return binTableRepository.save(existing);
    }

    public void delete(UUID id) {
        binTableRepository.deleteById(id);
    }

    public String resolveCardBrand(String pan) {
        if (pan == null || pan.length() < 6) return "ALL";
        for (int len : new int[]{8, 6}) {
            if (pan.length() >= len) {
                String bin = pan.substring(0, len);
                Optional<BinTable> match = binTableRepository.findByBinAndIsActiveTrue(bin);
                if (match.isPresent() && match.get().getCardBrand() != null) {
                    return match.get().getCardBrand().name();
                }
            }
        }
        return "ALL";
    }
}
