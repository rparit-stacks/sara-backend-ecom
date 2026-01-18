package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyDto {
    private String code;
    private String name;
    private String symbol;
    private BigDecimal rate;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CurrencyRatesResponse {
    private List<CurrencyDto> currencies;
    private Map<String, Double> rates;
}
