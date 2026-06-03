package com.switchplatform.platform.controller.issuing;

import com.switchplatform.platform.model.issuing.CardProduct;
import com.switchplatform.platform.model.issuing.CardProgram;
import com.switchplatform.platform.service.issuing.CardProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issuing/programs")
@RequiredArgsConstructor
public class CardProgramController {

    private final CardProgramService cardProgramService;

    @GetMapping
    public ResponseEntity<?> listPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(cardProgramService.listPrograms(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardProgram> getProgram(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.getProgram(id));
    }

    @PostMapping
    public ResponseEntity<CardProgram> createProgram(@Valid @RequestBody CardProgram program) {
        return ResponseEntity.ok(cardProgramService.createProgram(program));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardProgram> updateProgram(@PathVariable UUID id,
                                                       @Valid @RequestBody CardProgram update) {
        return ResponseEntity.ok(cardProgramService.updateProgram(id, update));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<CardProgram> activateProgram(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.activateProgram(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<CardProgram> deactivateProgram(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.deactivateProgram(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        cardProgramService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<CardProduct>> getProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.getProductsByProgram(id));
    }

    @PostMapping("/{programId}/products")
    public ResponseEntity<CardProduct> createProduct(@PathVariable UUID programId,
                                                       @Valid @RequestBody CardProduct product) {
        product.setProgramId(programId);
        return ResponseEntity.ok(cardProgramService.createProduct(product));
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(cardProgramService.listProducts(page, size));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<CardProduct> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.getProduct(id));
    }

    @GetMapping("/products/by-code/{code}")
    public ResponseEntity<CardProduct> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(cardProgramService.getProductByCode(code));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<CardProduct> updateProduct(@PathVariable UUID id,
                                                       @Valid @RequestBody CardProduct update) {
        return ResponseEntity.ok(cardProgramService.updateProduct(id, update));
    }

    @PostMapping("/products/{id}/activate")
    public ResponseEntity<CardProduct> activateProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.activateProduct(id));
    }

    @PostMapping("/products/{id}/deactivate")
    public ResponseEntity<CardProduct> deactivateProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(cardProgramService.deactivateProduct(id));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        cardProgramService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
