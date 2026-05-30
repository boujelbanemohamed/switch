package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.Terminal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalManagementService {

    private final ConcurrentMap<UUID, Terminal> terminalStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> tidIndex = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public Terminal registerTerminal(String merchantId, String terminalId, String type, String location) {
        if (tidIndex.containsKey(terminalId)) {
            throw new IllegalArgumentException("Terminal already exists: " + terminalId);
        }

        Terminal terminal = Terminal.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.fromString(merchantId))
                .terminalId(terminalId)
                .terminalType(Terminal.TerminalType.valueOf(type))
                .locationName(location)
                .status(Terminal.TerminalStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        terminalStore.put(terminal.getId(), terminal);
        tidIndex.put(terminalId, terminal.getId());
        log.info("TMS registered terminal {} (TID: {}) for merchant {}", terminal.getId(), terminalId, merchantId);
        return terminal;
    }

    public Terminal getTerminal(UUID id) {
        Terminal terminal = terminalStore.get(id);
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal not found: " + id);
        }
        return terminal;
    }

    public Terminal getByTid(String tid) {
        UUID uuid = tidIndex.get(tid);
        if (uuid == null) {
            throw new IllegalArgumentException("Terminal not found by TID: " + tid);
        }
        return getTerminal(uuid);
    }

    public Terminal updateStatus(String tid, String newStatus) {
        Terminal terminal = getByTid(tid);
        terminal.setStatus(Terminal.TerminalStatus.valueOf(newStatus));
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("TMS updated terminal {} status to {}", tid, newStatus);
        return terminal;
    }

    public Terminal updateKeys(String tid, String mKey, String pik, String mak) {
        Terminal terminal = getByTid(tid);
        terminal.setMKey(mKey);
        terminal.setPik(pik);
        terminal.setMak(mak);
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("TMS updated keys for terminal {}", tid);
        return terminal;
    }

    public Terminal rotateKeys(String tid) {
        Terminal terminal = getByTid(tid);
        terminal.setMKey(generateHexKey(32));
        terminal.setPik(generateHexKey(32));
        terminal.setMak(generateHexKey(32));
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("TMS rotated keys for terminal {}", tid);
        return terminal;
    }

    public List<Terminal> listByMerchant(String merchantId) {
        UUID merchantUuid = UUID.fromString(merchantId);
        return terminalStore.values().stream()
                .filter(t -> t.getMerchantId().equals(merchantUuid))
                .collect(Collectors.toList());
    }

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
