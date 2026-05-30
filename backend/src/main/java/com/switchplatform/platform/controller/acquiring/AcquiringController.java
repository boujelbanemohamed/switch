package com.switchplatform.platform.controller.acquiring;

import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.model.acquiring.NettingResult;
import com.switchplatform.platform.model.acquiring.SettlementRecord;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.service.acquiring.MerchantService;
import com.switchplatform.platform.service.acquiring.MerchantSettlementService;
import com.switchplatform.platform.service.acquiring.SettlementService;
import com.switchplatform.platform.service.acquiring.TerminalManagementService;
import com.switchplatform.platform.service.acquiring.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/acquiring")
@RequiredArgsConstructor
@Slf4j
public class AcquiringController {

    private final MerchantService merchantService;
    private final TerminalService terminalService;
    private final MerchantSettlementService settlementService;
    private final SettlementService settlementRecordService;
    private final TerminalManagementService terminalManagementService;

    @PostMapping("/merchants")
    public ResponseEntity<Merchant> onboardMerchant(@RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.onboardMerchant(merchant));
    }

    @GetMapping("/merchants/{id}")
    public ResponseEntity<Merchant> getMerchant(@PathVariable UUID id) {
        return merchantService.getMerchant(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchants/by-code/{code}")
    public ResponseEntity<Merchant> getMerchantByCode(@PathVariable String code) {
        return merchantService.getMerchantByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchants")
    public ResponseEntity<List<Merchant>> listMerchantsByStatus(
            @RequestParam(name = "status", required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(merchantService.listMerchantsByStatus(status));
        }
        return ResponseEntity.ok(merchantService.listMerchantsByStatus("ACTIVE"));
    }

    @PutMapping("/merchants/{id}")
    public ResponseEntity<Merchant> updateMerchant(@PathVariable UUID id, @RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.updateMerchant(id, merchant));
    }

    @PostMapping("/merchants/{id}/approve")
    public ResponseEntity<Merchant> approveMerchant(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.approveMerchant(id));
    }

    @PostMapping("/merchants/{id}/suspend")
    public ResponseEntity<Merchant> suspendMerchant(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(merchantService.suspendMerchant(id, body.get("reason")));
    }

    @PostMapping("/merchants/{id}/terminate")
    public ResponseEntity<Merchant> terminateMerchant(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.terminateMerchant(id));
    }

    @PostMapping("/terminals")
    public ResponseEntity<Terminal> registerTerminal(@RequestBody Terminal terminal) {
        return ResponseEntity.ok(terminalService.registerTerminal(terminal));
    }

    @GetMapping("/terminals/{id}")
    public ResponseEntity<Terminal> getTerminal(@PathVariable UUID id) {
        return terminalService.getTerminal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/terminals/by-tid/{tid}")
    public ResponseEntity<Terminal> getTerminalByTid(@PathVariable String tid) {
        return terminalService.getTerminalByTid(tid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchants/{merchantId}/terminals")
    public ResponseEntity<List<Terminal>> listMerchantTerminals(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(terminalService.listByMerchant(merchantId));
    }

    @PutMapping("/terminals/{id}/status")
    public ResponseEntity<Terminal> updateTerminalStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(terminalService.updateStatus(id, status));
    }

    @PostMapping("/merchants/{id}/mdr/calculate")
    public ResponseEntity<Map<String, BigDecimal>> calculateMdr(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String cardBrand = (String) body.get("cardBrand");
        String cardType = (String) body.get("cardType");
        BigDecimal fee = merchantService.calculateMdr(id, amount, cardBrand, cardType);
        return ResponseEntity.ok(Map.of("fee", fee));
    }

    @PostMapping("/settlements")
    public ResponseEntity<MerchantSettlement> createSettlement(@RequestBody Map<String, String> body) {
        UUID merchantId = UUID.fromString(body.get("merchantId"));
        LocalDate date = LocalDate.parse(body.get("settlementDate"));
        String currencyCode = body.get("currencyCode");
        return ResponseEntity.ok(settlementService.createSettlement(merchantId, date, currencyCode));
    }

    @PostMapping("/settlements/{id}/confirm")
    public ResponseEntity<MerchantSettlement> confirmSettlement(@PathVariable UUID id) {
        return ResponseEntity.ok(settlementService.confirmSettlement(id));
    }

    @PostMapping("/settlements/{id}/pay")
    public ResponseEntity<MerchantSettlement> markPaid(
            @PathVariable UUID id, @RequestParam String paymentRef) {
        return ResponseEntity.ok(settlementService.markPaid(id, paymentRef));
    }

    @GetMapping("/settlements/{id}")
    public ResponseEntity<MerchantSettlement> getSettlement(@PathVariable UUID id) {
        return settlementService.getSettlement(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/merchants/{merchantId}/settlements")
    public ResponseEntity<List<MerchantSettlement>> listMerchantSettlements(
            @PathVariable UUID merchantId,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        return ResponseEntity.ok(settlementService.getMerchantSettlements(merchantId, fromDate, toDate));
    }

    @GetMapping("/merchants/{merchantId}/netting")
    public ResponseEntity<NettingResult> calculateMerchantNetting(
            @PathVariable UUID merchantId,
            @RequestParam(name = "date") String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(settlementRecordService.calculateMerchantNetting(merchantId.toString(), localDate));
    }

    @PutMapping("/terminals/{tid}/keys")
    public ResponseEntity<Terminal> updateTerminalKeys(
            @PathVariable String tid,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(terminalManagementService.updateKeys(
                tid, body.get("mKey"), body.get("pik"), body.get("mak")));
    }

    @PostMapping("/terminals/{tid}/keys/rotate")
    public ResponseEntity<Terminal> rotateTerminalKeys(@PathVariable String tid) {
        return ResponseEntity.ok(terminalManagementService.rotateKeys(tid));
    }

    @GetMapping("/terminals/{tid}/status")
    public ResponseEntity<Map<String, Object>> getTerminalStatus(@PathVariable String tid) {
        return ResponseEntity.ok(terminalManagementService.getStatus(tid));
    }
}
