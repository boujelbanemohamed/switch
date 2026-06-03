package com.switchplatform.platform.controller.issuing;

import com.switchplatform.platform.model.issuing.VirtualCard;
import com.switchplatform.platform.service.issuing.VirtualCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issuing/virtual-cards")
@RequiredArgsConstructor
public class VirtualCardController {

    private final VirtualCardService virtualCardService;

    @GetMapping
    public ResponseEntity<?> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(virtualCardService.listAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VirtualCard> getCard(@PathVariable UUID id) {
        return ResponseEntity.ok(virtualCardService.getCard(id));
    }

    @GetMapping("/by-external/{externalId}")
    public ResponseEntity<VirtualCard> getByExternalId(@PathVariable String externalId) {
        return ResponseEntity.ok(virtualCardService.getCardByExternalId(externalId));
    }

    @GetMapping("/by-cardholder/{cardholderId}")
    public ResponseEntity<List<VirtualCard>> getByCardholder(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(virtualCardService.getCardsByCardholder(cardholderId));
    }

    @GetMapping("/by-funding/{fundingCardId}")
    public ResponseEntity<List<VirtualCard>> getByFundingCard(@PathVariable UUID fundingCardId) {
        return ResponseEntity.ok(virtualCardService.getCardsByFundingCard(fundingCardId));
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<VirtualCard>> getByStatus(@PathVariable VirtualCard.Status status) {
        return ResponseEntity.ok(virtualCardService.listByStatus(status));
    }

    @PostMapping
    public ResponseEntity<VirtualCard> createCard(@Valid @RequestBody VirtualCard card) {
        return ResponseEntity.ok(virtualCardService.createVirtualCard(card));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<VirtualCard> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(virtualCardService.activateCard(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<VirtualCard> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(virtualCardService.suspendCard(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<VirtualCard> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(virtualCardService.resumeCard(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<VirtualCard> cancel(@PathVariable UUID id,
                                               @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(virtualCardService.cancelCard(id, reason));
    }

    @PutMapping("/{id}/limits")
    public ResponseEntity<VirtualCard> updateLimits(@PathVariable UUID id,
                                                     @RequestParam(required = false) BigDecimal amountLimit,
                                                     @RequestParam(required = false) Integer maxTransactions) {
        return ResponseEntity.ok(virtualCardService.updateLimits(id, amountLimit, maxTransactions));
    }
}
