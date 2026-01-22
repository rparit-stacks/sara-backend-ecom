package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public class CustomConfigRequest {
    private String pageTitle;
    private String pageDescription;
    private String uploadLabel;
    private BigDecimal designPrice;
    private Integer minQuantity;
    private Integer maxQuantity;
    private String termsAndConditions;
    
    // UI Text Fields
    private String uploadButtonText;
    private String continueButtonText;
    private String submitButtonText;
    private String addToCartButtonText;
    private String selectFabricLabel;
    private String quantityLabel;
    private String instructions;
    
    // Business Logic Fields
    private BigDecimal gstRate;
    private String hsnCode;
    private List<Long> recommendedFabricIds;
    
    // Variants and Pricing Slabs
    private List<VariantRequest> variants;
    private List<PricingSlabRequest> pricingSlabs;
    
    // Form Fields
    private List<FormFieldRequest> formFields;
    
    public static class FormFieldRequest {
        private String type;
        private String label;
        private String placeholder;
        private Boolean required;
        private Integer minValue;
        private Integer maxValue;
        private List<String> options;
        private Integer displayOrder;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getPlaceholder() { return placeholder; }
        public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
        public Integer getMinValue() { return minValue; }
        public void setMinValue(Integer minValue) { this.minValue = minValue; }
        public Integer getMaxValue() { return maxValue; }
        public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
    
    // Getters and Setters
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
    public String getPageDescription() { return pageDescription; }
    public void setPageDescription(String pageDescription) { this.pageDescription = pageDescription; }
    public String getUploadLabel() { return uploadLabel; }
    public void setUploadLabel(String uploadLabel) { this.uploadLabel = uploadLabel; }
    public BigDecimal getDesignPrice() { return designPrice; }
    public void setDesignPrice(BigDecimal designPrice) { this.designPrice = designPrice; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }
    
    // UI Text Fields Getters and Setters
    public String getUploadButtonText() { return uploadButtonText; }
    public void setUploadButtonText(String uploadButtonText) { this.uploadButtonText = uploadButtonText; }
    public String getContinueButtonText() { return continueButtonText; }
    public void setContinueButtonText(String continueButtonText) { this.continueButtonText = continueButtonText; }
    public String getSubmitButtonText() { return submitButtonText; }
    public void setSubmitButtonText(String submitButtonText) { this.submitButtonText = submitButtonText; }
    public String getAddToCartButtonText() { return addToCartButtonText; }
    public void setAddToCartButtonText(String addToCartButtonText) { this.addToCartButtonText = addToCartButtonText; }
    public String getSelectFabricLabel() { return selectFabricLabel; }
    public void setSelectFabricLabel(String selectFabricLabel) { this.selectFabricLabel = selectFabricLabel; }
    public String getQuantityLabel() { return quantityLabel; }
    public void setQuantityLabel(String quantityLabel) { this.quantityLabel = quantityLabel; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    // Business Logic Fields Getters and Setters
    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }
    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
    public List<Long> getRecommendedFabricIds() { return recommendedFabricIds; }
    public void setRecommendedFabricIds(List<Long> recommendedFabricIds) { this.recommendedFabricIds = recommendedFabricIds; }
    
    // Variants and Pricing Slabs Getters and Setters
    public List<VariantRequest> getVariants() { return variants; }
    public void setVariants(List<VariantRequest> variants) { this.variants = variants; }
    public List<PricingSlabRequest> getPricingSlabs() { return pricingSlabs; }
    public void setPricingSlabs(List<PricingSlabRequest> pricingSlabs) { this.pricingSlabs = pricingSlabs; }
    
    // Form Fields Getters and Setters
    public List<FormFieldRequest> getFormFields() { return formFields; }
    public void setFormFields(List<FormFieldRequest> formFields) { this.formFields = formFields; }
    
    public static class VariantRequest {
        private String type;
        private String name;
        private String unit;
        private String frontendId;
        private Integer displayOrder;
        private List<VariantOptionRequest> options;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getFrontendId() { return frontendId; }
        public void setFrontendId(String frontendId) { this.frontendId = frontendId; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        public List<VariantOptionRequest> getOptions() { return options; }
        public void setOptions(List<VariantOptionRequest> options) { this.options = options; }
    }
    
    public static class VariantOptionRequest {
        private String value;
        private String frontendId;
        private BigDecimal priceModifier;
        private Integer displayOrder;
        
        // Getters and Setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getFrontendId() { return frontendId; }
        public void setFrontendId(String frontendId) { this.frontendId = frontendId; }
        public BigDecimal getPriceModifier() { return priceModifier; }
        public void setPriceModifier(BigDecimal priceModifier) { this.priceModifier = priceModifier; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
    
    public static class PricingSlabRequest {
        private Integer minQuantity;
        private Integer maxQuantity;
        private String discountType; // FIXED_AMOUNT or PERCENTAGE
        private BigDecimal discountValue;
        private Integer displayOrder;
        
        // Getters and Setters
        public Integer getMinQuantity() { return minQuantity; }
        public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
        public Integer getMaxQuantity() { return maxQuantity; }
        public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
        public String getDiscountType() { return discountType; }
        public void setDiscountType(String discountType) { this.discountType = discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
}
