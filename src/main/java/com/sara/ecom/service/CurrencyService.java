package com.sara.ecom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.CurrencyDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private static final String BASE_CURRENCY = "INR";
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    // Cache for exchange rates (currency code -> rate)
    private final Map<String, Double> exchangeRatesCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour
    
    @PostConstruct
    public void init() {
        logger.info("Initializing CurrencyService, fetching exchange rates...");
        fetchAndCacheRates();
    }
    
    @Value("${currency.api.provider:exchangerate-api}")
    private String currencyApiProvider;
    
    /**
     * Get all exchange rates from API
     */
    public Map<String, Double> getExchangeRates() {
        // Check if cache is still valid
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate < CACHE_DURATION_MS && !exchangeRatesCache.isEmpty()) {
            return new HashMap<>(exchangeRatesCache);
        }
        
        // Fetch fresh rates
        try {
            fetchAndCacheRates();
            return new HashMap<>(exchangeRatesCache);
        } catch (Exception e) {
            logger.error("Failed to fetch exchange rates, using cached values", e);
            // Return cached values even if stale
            return new HashMap<>(exchangeRatesCache);
        }
    }
    
    /**
     * Fetch rates from ExchangeRate-API and cache them
     */
    private void fetchAndCacheRates() {
        try {
            String apiKey = getCurrencyApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Currency API key not configured, using default rates");
                loadDefaultRates();
                return;
            }
            
            String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + BASE_CURRENCY;
            logger.info("Fetching exchange rates from: {}", url.replace(apiKey, "***"));
            
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                throw new RuntimeException("Empty response from currency API");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            String result = jsonNode.get("result").asText();
            
            if (!"success".equals(result)) {
                logger.error("Currency API returned error: {}", jsonNode.get("error-type").asText("unknown"));
                loadDefaultRates();
                return;
            }
            
            JsonNode ratesNode = jsonNode.get("conversion_rates");
            exchangeRatesCache.clear();
            
            // Store rates (all rates are relative to INR)
            Iterator<Map.Entry<String, JsonNode>> fields = ratesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String currency = entry.getKey();
                double rate = entry.getValue().asDouble();
                exchangeRatesCache.put(currency, rate);
            }
            
            // Always include base currency with rate 1.0
            exchangeRatesCache.put(BASE_CURRENCY, 1.0);
            
            lastCacheUpdate = System.currentTimeMillis();
            logger.info("Successfully cached {} exchange rates", exchangeRatesCache.size());
            
        } catch (Exception e) {
            logger.error("Error fetching exchange rates from API", e);
            loadDefaultRates();
        }
    }
    
    /**
     * Load default rates if API fails
     */
    private void loadDefaultRates() {
        exchangeRatesCache.clear();
        exchangeRatesCache.put("INR", 1.0);
        exchangeRatesCache.put("USD", 0.012);
        exchangeRatesCache.put("EUR", 0.011);
        exchangeRatesCache.put("GBP", 0.0095);
        exchangeRatesCache.put("JPY", 1.8);
        exchangeRatesCache.put("AUD", 0.018);
        exchangeRatesCache.put("CAD", 0.016);
        exchangeRatesCache.put("CHF", 0.0105);
        exchangeRatesCache.put("CNY", 0.086);
        exchangeRatesCache.put("AED", 0.044);
        exchangeRatesCache.put("SAR", 0.045);
        exchangeRatesCache.put("SGD", 0.016);
        lastCacheUpdate = System.currentTimeMillis();
    }
    
    /**
     * Get currency API key from BusinessConfig
     */
    private String getCurrencyApiKey() {
        try {
            var config = businessConfigService.getConfigEntity();
            return config != null ? config.getCurrencyApiKey() : null;
        } catch (Exception e) {
            logger.error("Error getting currency API key from config", e);
            return null;
        }
    }
    
    /**
     * Convert amount from one currency to another
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        Map<String, Double> rates = getExchangeRates();
        
        // If converting from base currency (INR)
        if (BASE_CURRENCY.equals(fromCurrency)) {
            Double rate = rates.get(toCurrency);
            if (rate == null) {
                logger.warn("Exchange rate not found for currency: {}, returning original amount", toCurrency);
                return amount;
            }
            return amount.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
        }
        
        // If converting to base currency (INR)
        if (BASE_CURRENCY.equals(toCurrency)) {
            Double rate = rates.get(fromCurrency);
            if (rate == null) {
                logger.warn("Exchange rate not found for currency: {}, returning original amount", fromCurrency);
                return amount;
            }
            return amount.divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP);
        }
        
        // Converting between two non-base currencies
        // First convert to INR, then to target currency
        Double fromRate = rates.get(fromCurrency);
        Double toRate = rates.get(toCurrency);
        
        if (fromRate == null || toRate == null) {
            logger.warn("Exchange rate not found for currencies: {} or {}, returning original amount", fromCurrency, toCurrency);
            return amount;
        }
        
        BigDecimal inrAmount = amount.divide(BigDecimal.valueOf(fromRate), 4, RoundingMode.HALF_UP);
        return inrAmount.multiply(BigDecimal.valueOf(toRate)).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get list of available currencies with their rates
     */
    public List<CurrencyDto> getAvailableCurrencies() {
        Map<String, Double> rates = getExchangeRates();
        List<CurrencyDto> currencies = new ArrayList<>();
        
        // Common currencies to display
        String[] commonCurrencies = {
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY",
            "AED", "SAR", "SGD", "MYR", "THB", "IDR", "PHP", "KRW"
        };
        
        Map<String, String> currencyNames = getCurrencyNames();
        Map<String, String> currencySymbols = getCurrencySymbols();
        
        for (String code : commonCurrencies) {
            Double rate = rates.get(code);
            if (rate != null) {
                currencies.add(CurrencyDto.builder()
                    .code(code)
                    .name(currencyNames.getOrDefault(code, code))
                    .symbol(currencySymbols.getOrDefault(code, code))
                    .rate(BigDecimal.valueOf(rate))
                    .build());
            }
        }
        
        return currencies;
    }
    
    /**
     * Scheduled task to refresh exchange rates every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void refreshRates() {
        logger.info("Scheduled refresh of exchange rates");
        fetchAndCacheRates();
    }
    
    private Map<String, String> getCurrencyNames() {
        Map<String, String> names = new HashMap<>();
        names.put("INR", "Indian Rupee");
        names.put("USD", "US Dollar");
        names.put("EUR", "Euro");
        names.put("GBP", "British Pound");
        names.put("JPY", "Japanese Yen");
        names.put("AUD", "Australian Dollar");
        names.put("CAD", "Canadian Dollar");
        names.put("CHF", "Swiss Franc");
        names.put("CNY", "Chinese Yuan");
        names.put("AED", "UAE Dirham");
        names.put("SAR", "Saudi Riyal");
        names.put("SGD", "Singapore Dollar");
        names.put("MYR", "Malaysian Ringgit");
        names.put("THB", "Thai Baht");
        names.put("IDR", "Indonesian Rupiah");
        names.put("PHP", "Philippine Peso");
        names.put("KRW", "South Korean Won");
        return names;
    }
    
    private Map<String, String> getCurrencySymbols() {
        Map<String, String> symbols = new HashMap<>();
        symbols.put("INR", "₹");
        symbols.put("USD", "$");
        symbols.put("EUR", "€");
        symbols.put("GBP", "£");
        symbols.put("JPY", "¥");
        symbols.put("AUD", "A$");
        symbols.put("CAD", "C$");
        symbols.put("CHF", "CHF");
        symbols.put("CNY", "¥");
        symbols.put("AED", "د.إ");
        symbols.put("SAR", "﷼");
        symbols.put("SGD", "S$");
        symbols.put("MYR", "RM");
        symbols.put("THB", "฿");
        symbols.put("IDR", "Rp");
        symbols.put("PHP", "₱");
        symbols.put("KRW", "₩");
        return symbols;
    }
}
