package com.switchplatform.platform.service.routing;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.repository.BinTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
        existing.setBin(binTable.getBin());
        existing.setCardBrand(binTable.getCardBrand());
        existing.setParticipant(binTable.getParticipant());
        existing.setIsActive(binTable.getIsActive());
        return binTableRepository.save(existing);
    }

    public void delete(UUID id) {
        binTableRepository.deleteById(id);
    }
}
