package com.sara.ecom.service;

import com.sara.ecom.entity.CurrencyMultiplier;
import com.sara.ecom.repository.CurrencyMultiplierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyMultiplierService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyMultiplierService.class);

    private static final String BASE_CURRENCY = "INR";

    private final CurrencyMultiplierRepository currencyMultiplierRepository;

    /**
     * Get multiplier for a given currency code.
     * - INR is always 1.
     * - If not found or invalid, falls back to 1.
     */
    public BigDecimal getMultiplier(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return BigDecimal.ONE;
        }

        String normalized = currencyCode.trim().toUpperCase();

        // For INR (India), always return 1 (no change)
        if (BASE_CURRENCY.equals(normalized)) {
            return BigDecimal.ONE;
        }

        try {
            return currencyMultiplierRepository.findByCurrencyCodeIgnoreCase(normalized)
                    .map(CurrencyMultiplier::getMultiplier)
                    .filter(m -> m != null && m.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(BigDecimal.ONE);
        } catch (Exception e) {
            logger.error("Error fetching multiplier for currency: {}", normalized, e);
            return BigDecimal.ONE;
        }
    }

    /**
     * Get all configured multipliers as a simple map.
     */
    public Map<String, BigDecimal> getAllMultipliers() {
        Map<String, BigDecimal> result = new HashMap<>();
        try {
            List<CurrencyMultiplier> all = currencyMultiplierRepository.findAll();
            for (CurrencyMultiplier cm : all) {
                if (cm.getCurrencyCode() == null || cm.getMultiplier() == null) {
                    continue;
                }
                String code = cm.getCurrencyCode().trim().toUpperCase();
                if (code.isEmpty()) {
                    continue;
                }
                // Always treat INR as 1 regardless of stored value to avoid accidental changes
                if (BASE_CURRENCY.equals(code)) {
                    result.put(code, BigDecimal.ONE);
                } else if (cm.getMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                    result.put(code, cm.getMultiplier());
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching all currency multipliers", e);
        }

        // Ensure INR is always present with multiplier 1
        result.putIfAbsent(BASE_CURRENCY, BigDecimal.ONE);

        return result;
    }
}

