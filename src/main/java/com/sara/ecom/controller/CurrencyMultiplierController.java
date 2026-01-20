package com.sara.ecom.controller;

import com.sara.ecom.service.CurrencyMultiplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyMultiplierController {

    private final CurrencyMultiplierService currencyMultiplierService;

    /**
     * Get all configured currency multipliers.
     * Example response:
     * {
     *   "multipliers": {
     *     "USD": 2.0,
     *     "EUR": 1.2,
     *     "INR": 1.0
     *   }
     * }
     */
    @GetMapping("/multipliers")
    public ResponseEntity<Map<String, Object>> getMultipliers() {
        Map<String, BigDecimal> multipliers = currencyMultiplierService.getAllMultipliers();

        Map<String, Object> response = new HashMap<>();
        response.put("multipliers", multipliers);

        return ResponseEntity.ok(response);
    }
}

