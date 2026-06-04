package com.switchplatform.platform.controller.fx;

import com.switchplatform.platform.model.fx.FxRate;
import com.switchplatform.platform.service.fx.FxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fx")
@RequiredArgsConstructor
public class FxController {

    private final FxService fxService;

    @GetMapping("/rates")
    public ResponseEntity<List<FxRate>> listRates() {
        return ResponseEntity.ok(fxService.listRates());
    }

    @PostMapping("/rates")
    public ResponseEntity<FxRate> createRate(@Valid @RequestBody FxRate rate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fxService.createRate(rate));
    }

    @GetMapping("/rates/{id}")
    public ResponseEntity<FxRate> getRate(@PathVariable UUID id) {
        return ResponseEntity.ok(fxService.getRate(id));
    }

    @PutMapping("/rates/{id}")
    public ResponseEntity<FxRate> updateRate(@PathVariable UUID id, @Valid @RequestBody FxRate rate) {
        return ResponseEntity.ok(fxService.updateRate(id, rate));
    }

    @DeleteMapping("/rates/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable UUID id) {
        fxService.deleteRate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(@RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String source = (String) body.get("sourceCurrency");
        String target = (String) body.get("targetCurrency");
        BigDecimal result = fxService.convert(amount, source, target);
        return ResponseEntity.ok(Map.of(
                "amount", amount,
                "sourceCurrency", source,
                "targetCurrency", target,
                "convertedAmount", result));
    }

    @PostMapping("/dcc/propose")
    public ResponseEntity<Map<String, Object>> proposeDcc(@RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String source = (String) body.get("sourceCurrency");
        String target = (String) body.get("targetCurrency");
        BigDecimal dccAmount = fxService.proposeDcc(amount, source, target);
        return ResponseEntity.ok(Map.of(
                "amount", amount,
                "sourceCurrency", source,
                "targetCurrency", target,
                "dccAmount", dccAmount));
    }
}
