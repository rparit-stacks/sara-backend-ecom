package com.sara.ecom.dto;

import java.math.BigDecimal;

/**
 * Structured variant selection data for cart and orders.
 * Contains both database IDs and frontendIds for reliable mapping.
 */
public class VariantSelectionDto {
    private Long variantId;
    private String variantFrontendId;
    private String variantName;
    private String variantType;
    private String variantUnit;
    
    private Long optionId;
    private String optionFrontendId;
    private String optionValue;
    private BigDecimal priceModifier;
    
    // Getters and Setters
    public Long getVariantId() {
        return variantId;
    }
    
    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }
    
    public String getVariantFrontendId() {
        return variantFrontendId;
    }
    
    public void setVariantFrontendId(String variantFrontendId) {
        this.variantFrontendId = variantFrontendId;
    }
    
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
    
    public Long getOptionId() {
        return optionId;
    }
    
    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
    
    public String getOptionFrontendId() {
        return optionFrontendId;
    }
    
    public void setOptionFrontendId(String optionFrontendId) {
        this.optionFrontendId = optionFrontendId;
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
