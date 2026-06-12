package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.repository.acquiring.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalManagementService {

    private final TerminalRepository terminalRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Terminal registerTerminal(String merchantId, String terminalId, String type, String location) {
        if (terminalRepository.existsByTerminalId(terminalId)) {
            throw new IllegalArgumentException("Terminal already exists: " + terminalId);
        }

        Terminal terminal = Terminal.builder()
                .merchantId(UUID.fromString(merchantId))
                .terminalId(terminalId)
                .terminalType(Terminal.TerminalType.valueOf(type))
                .locationName(location)
                .status(Terminal.TerminalStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        terminal = terminalRepository.save(terminal);
        log.info("TMS registered terminal {} (TID: {}) for merchant {}", terminal.getId(), terminalId, merchantId);
        return terminal;
    }

    @Transactional(readOnly = true)
    public Terminal getTerminal(UUID id) {
        return terminalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + id));
    }

    @Transactional(readOnly = true)
    public Terminal getByTid(String tid) {
        return terminalRepository.findByTerminalId(tid)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found by TID: " + tid));
    }

    @Transactional
    public Terminal updateStatus(String tid, String newStatus) {
        Terminal terminal = getByTid(tid);
        terminal.setStatus(Terminal.TerminalStatus.valueOf(newStatus));
        terminal.setUpdatedAt(OffsetDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("TMS updated terminal {} status to {}", tid, newStatus);
        return terminal;
    }

    @Transactional
    public Terminal updateKeys(String tid, String mKey, String pik, String mak) {
        Terminal terminal = getByTid(tid);
        terminal.setMKey(mKey);
        terminal.setPik(pik);
        terminal.setMak(mak);
        terminal.setUpdatedAt(OffsetDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("TMS updated keys for terminal {}", tid);
        return terminal;
    }

    @Transactional
    public Terminal rotateKeys(String tid) {
        Terminal terminal = getByTid(tid);
        terminal.setMKey(generateHexKey(32));
        terminal.setPik(generateHexKey(32));
        terminal.setMak(generateHexKey(32));
        terminal.setUpdatedAt(OffsetDateTime.now());
        terminal = terminalRepository.save(terminal);
        log.info("TMS rotated keys for terminal {}", tid);
        return terminal;
    }

    @Transactional(readOnly = true)
    public List<Terminal> listByMerchant(String merchantId) {
        UUID merchantUuid = UUID.fromString(merchantId);
        return terminalRepository.findByMerchantId(merchantUuid);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(String tid) {
        Terminal terminal = getByTid(tid);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("terminalId", terminal.getTerminalId());
        status.put("status", terminal.getStatus().name());
        status.put("locationName", terminal.getLocationName());
        status.put("serialNumber", terminal.getSerialNumber());
        status.put("manufacturer", terminal.getManufacturer());
        status.put("model", terminal.getModel());
        status.put("firmwareVersion", terminal.getFirmwareVersion());
        status.put("lastActivityAt", terminal.getLastActivityAt() != null ? terminal.getLastActivityAt().toString() : null);
        status.put("lastContact", terminal.getLastContact() != null ? terminal.getLastContact().toString() : null);
        return status;
    }

    private String generateHexKey(int length) {
        byte[] bytes = new byte[length / 2];
        secureRandom.nextBytes(bytes);
        return Hex.encodeHexString(bytes).toUpperCase();
    }
}
