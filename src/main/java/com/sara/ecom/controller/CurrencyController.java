package com.sara.ecom.controller;

import com.sara.ecom.dto.CurrencyDto;
import com.sara.ecom.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://design-observer-pro-cyan.vercel.app", "https://studiosara.in", "http://studiosara.in", "https://www.studiosara.in", "http://www.studiosara.in"})
public class CurrencyController {
    
    private final CurrencyService currencyService;
    
    /**
     * Get all exchange rates
     */
    @GetMapping("/rates")
    public ResponseEntity<Map<String, Object>> getRates() {
        Map<String, Double> rates = currencyService.getExchangeRates();
        List<CurrencyDto> currencies = currencyService.getAvailableCurrencies();
        
        Map<String, Object> response = new HashMap<>();
        response.put("rates", rates);
        response.put("currencies", currencies);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Convert amount from one currency to another
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "INR") String from,
            @RequestParam String to) {
        
        BigDecimal converted = currencyService.convert(amount, from, to);
        
        Map<String, Object> response = new HashMap<>();
        response.put("amount", amount);
        response.put("converted", converted);
        response.put("from", from);
        response.put("to", to);
        
        return ResponseEntity.ok(response);
    }
}
