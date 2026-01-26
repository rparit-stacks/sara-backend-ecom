package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public class PlainProductRequest {
    private String name;
    private String description;
    private String image;
    private BigDecimal pricePerMeter;
    private String unitExtension;
    private Long categoryId;
    private String status;
    private List<VariantRequest> variants;
    
    public static class VariantRequest {
        private String type;
        private String name;
        private List<OptionRequest> options;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<OptionRequest> getOptions() {
            return options;
        }
        
        public void setOptions(List<OptionRequest> options) {
            this.options = options;
        }
    }
    
    public static class OptionRequest {
        private String value;
        private BigDecimal priceModifier;
        private Integer displayOrder;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public BigDecimal getPriceModifier() {
            return priceModifier;
        }
        
        public void setPriceModifier(BigDecimal priceModifier) {
            this.priceModifier = priceModifier;
        }
        
        public Integer getDisplayOrder() {
            return displayOrder;
        }
        
        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public BigDecimal getPricePerMeter() {
        return pricePerMeter;
    }
    
    public void setPricePerMeter(BigDecimal pricePerMeter) {
        this.pricePerMeter = pricePerMeter;
    }
    
    public String getUnitExtension() {
        return unitExtension;
    }
    
    public void setUnitExtension(String unitExtension) {
        this.unitExtension = unitExtension;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<VariantRequest> getVariants() {
        return variants;
    }
    
    public void setVariants(List<VariantRequest> variants) {
        this.variants = variants;
    }
}
