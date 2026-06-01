package com.switchplatform.platform.controller.merchant;

import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.service.merchant.MerchantPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant-portal")
@RequiredArgsConstructor
public class MerchantPortalController {

    private final MerchantPortalService merchantPortalService;

    private void checkAccess(String merchantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthorized = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                           || a.getAuthority().equals("ROLE_OPERATOR")
                           || a.getAuthority().equals("ROLE_ANALYST"));
        if (!isAuthorized) {
            throw new AccessDeniedException("Access denied to merchant: " + merchantCode);
        }
    }

    @GetMapping("/dashboard/{merchantCode}")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable String merchantCode) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getDashboard(merchantCode));
    }

    @GetMapping("/transactions/{merchantCode}")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @PathVariable String merchantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getTransactions(merchantCode, page, size));
    }

    @GetMapping("/terminals/{merchantCode}")
    public ResponseEntity<List<Terminal>> getTerminals(@PathVariable String merchantCode) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getTerminals(merchantCode));
    }

    @GetMapping("/settlements/{merchantCode}")
    public ResponseEntity<List<MerchantSettlement>> getSettlements(@PathVariable String merchantCode) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getSettlements(merchantCode));
    }

    @PostMapping("/refunds/{merchantCode}")
    public ResponseEntity<EpgTransaction> refundTransaction(
            @PathVariable String merchantCode,
            @RequestBody Map<String, String> request) {
        checkAccess(merchantCode);
        UUID epgTransactionId = UUID.fromString(request.get("epgTransactionId"));
        return ResponseEntity.ok(merchantPortalService.refundEpgTransaction(merchantCode, epgTransactionId));
    }

    @GetMapping("/reports/{merchantCode}")
    public ResponseEntity<Map<String, Object>> getReport(
            @PathVariable String merchantCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getReportData(merchantCode, from, to));
    }

    @GetMapping("/info/{merchantCode}")
    public ResponseEntity<Map<String, Object>> getInfo(@PathVariable String merchantCode) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getInfo(merchantCode));
    }

    @GetMapping("/stats/{merchantCode}")
    public ResponseEntity<List<Map<String, Object>>> getDailyStats(
            @PathVariable String merchantCode,
            @RequestParam(defaultValue = "30") int days) {
        checkAccess(merchantCode);
        return ResponseEntity.ok(merchantPortalService.getDailyStats(merchantCode, days));
    }
}
