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
}
