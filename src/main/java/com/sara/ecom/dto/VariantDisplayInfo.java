package com.sara.ecom.dto;

import java.math.BigDecimal;

/**
 * Display information for a variant selection in orders.
 * Contains resolved names for better admin display.
 */
public class VariantDisplayInfo {
    private String variantName;
    private String variantType;
    private String variantUnit;
    private String optionValue;
    private BigDecimal priceModifier;
    
    // Getters and Setters
    public String getVariantName() {
        return variantName;
    }
    
    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }
    
    public String getVariantType() {
        return variantType;
    }
    
    public void setVariantType(String variantType) {
        this.variantType = variantType;
    }
    
    public String getVariantUnit() {
        return variantUnit;
    }
    
    public void setVariantUnit(String variantUnit) {
        this.variantUnit = variantUnit;
    }
    
    public String getOptionValue() {
        return optionValue;
    }
    
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }
    
    public BigDecimal getPriceModifier() {
        return priceModifier;
    }
    
    public void setPriceModifier(BigDecimal priceModifier) {
        this.priceModifier = priceModifier;
    }
}
