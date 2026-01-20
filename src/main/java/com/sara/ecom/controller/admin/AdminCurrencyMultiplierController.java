package com.sara.ecom.controller.admin;

import com.sara.ecom.entity.CurrencyMultiplier;
import com.sara.ecom.repository.CurrencyMultiplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/currency-multipliers")
@RequiredArgsConstructor
public class AdminCurrencyMultiplierController {

    private final CurrencyMultiplierRepository currencyMultiplierRepository;

    @GetMapping
    public ResponseEntity<List<CurrencyMultiplier>> getAll() {
        List<CurrencyMultiplier> all = currencyMultiplierRepository.findAll();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    public ResponseEntity<CurrencyMultiplier> create(@RequestBody CurrencyMultiplier request) {
        String code = request.getCurrencyCode() != null ? request.getCurrencyCode().trim().toUpperCase() : null;
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        BigDecimal multiplier = request.getMultiplier();
        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            multiplier = BigDecimal.ONE;
        }

        CurrencyMultiplier entity = currencyMultiplierRepository
                .findByCurrencyCodeIgnoreCase(code)
                .orElseGet(CurrencyMultiplier::new);

        entity.setCurrencyCode(code);
        entity.setMultiplier(multiplier);

        CurrencyMultiplier saved = currencyMultiplierRepository.save(entity);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CurrencyMultiplier> update(
            @PathVariable Long id,
            @RequestBody CurrencyMultiplier request
    ) {
        return currencyMultiplierRepository.findById(id)
                .map(existing -> {
                    if (request.getCurrencyCode() != null) {
                        String code = request.getCurrencyCode().trim().toUpperCase();
                        if (!code.isEmpty()) {
                            existing.setCurrencyCode(code);
                        }
                    }
                    if (request.getMultiplier() != null && request.getMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                        existing.setMultiplier(request.getMultiplier());
                    }
                    CurrencyMultiplier saved = currencyMultiplierRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!currencyMultiplierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        currencyMultiplierRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

