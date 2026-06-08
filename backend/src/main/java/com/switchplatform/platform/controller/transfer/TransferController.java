package com.switchplatform.platform.controller.transfer;

import com.switchplatform.platform.model.transfer.Transfer;
import com.switchplatform.platform.model.transfer.TransferBeneficiary;
import com.switchplatform.platform.model.transfer.TransferLimit;
import com.switchplatform.platform.repository.transfer.TransferBeneficiaryRepository;
import com.switchplatform.platform.repository.transfer.TransferLimitRepository;
import com.switchplatform.platform.repository.transfer.TransferRepository;
import com.switchplatform.platform.service.transfer.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransferRepository transferRepository;
    private final TransferLimitRepository limitRepository;
    private final TransferBeneficiaryRepository beneficiaryRepository;

    @PostMapping("/a2a")
    public ResponseEntity<Transfer> executeA2A(@RequestBody Map<String, Object> body) {
        UUID sourceId = UUID.fromString((String) body.get("sourceAccountId"));
        UUID destId = UUID.fromString((String) body.get("destinationAccountId"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.getOrDefault("currency", "TND");
        String channel = (String) body.getOrDefault("channel", "BACKOFFICE");
        Transfer result = transferService.executeA2A(sourceId, destId, amount, currency, channel);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/p2p")
    public ResponseEntity<Transfer> executeP2P(@RequestBody Map<String, Object> body) {
        String sourcePan = (String) body.get("sourcePan");
        String destinationRef = (String) body.get("destinationRef");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.getOrDefault("currency", "TND");
        String channel = (String) body.getOrDefault("channel", "BACKOFFICE");
        Transfer result = transferService.executeP2P(sourcePan, destinationRef, amount, currency, channel);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<Transfer> reverseTransfer(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Manual reversal");
        Transfer result = transferService.reverseTransfer(id, reason);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll(Pageable pageable) {
        List<Transfer> transfers = transferRepository.findAll(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        return ResponseEntity.ok(Map.of("content", transfers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transfer> getTransfer(@PathVariable UUID id) {
        return transferRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transfer>> listByAccount(@PathVariable UUID accountId) {
        List<Transfer> transfers = transferRepository
                .findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(accountId, accountId);
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/limits")
    public ResponseEntity<List<TransferLimit>> listLimits() {
        return ResponseEntity.ok(limitRepository.findAll());
    }

    @PutMapping("/limits/{id}")
    public ResponseEntity<TransferLimit> updateLimit(@PathVariable UUID id, @RequestBody TransferLimit limit) {
        return limitRepository.findById(id)
                .map(existing -> {
                    if (limit.getPerTransferMax() != null) existing.setPerTransferMax(limit.getPerTransferMax());
                    if (limit.getDailyMaxAmount() != null) existing.setDailyMaxAmount(limit.getDailyMaxAmount());
                    if (limit.getDailyMaxCount() != null) existing.setDailyMaxCount(limit.getDailyMaxCount());
                    if (limit.getCurrencyCode() != null) existing.setCurrencyCode(limit.getCurrencyCode());
                    if (limit.getStatus() != null) existing.setStatus(limit.getStatus());
                    return ResponseEntity.ok(limitRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/beneficiaries")
    public ResponseEntity<List<TransferBeneficiary>> listBeneficiaries(
            @RequestParam(required = false) UUID ownerCardholderId) {
        if (ownerCardholderId != null) {
            return ResponseEntity.ok(beneficiaryRepository.findByOwnerCardholderIdOrderByCreatedAtDesc(ownerCardholderId));
        }
        return ResponseEntity.ok(beneficiaryRepository.findAll());
    }

    @PostMapping("/beneficiaries")
    public ResponseEntity<TransferBeneficiary> createBeneficiary(@RequestBody TransferBeneficiary beneficiary) {
        return ResponseEntity.ok(beneficiaryRepository.save(beneficiary));
    }
}
