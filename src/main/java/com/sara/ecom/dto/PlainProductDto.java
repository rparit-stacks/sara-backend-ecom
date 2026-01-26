package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public class PlainProductDto {
    private Long id;
    private String name;
    private String description;
    private String image;
    private BigDecimal pricePerMeter;
    private String unitExtension;
    private Long categoryId;
    private String status;
    private List<VariantDto> variants;
    
    public static class VariantDto {
        private Long id;
        private String type;
        private String name;
        private List<OptionDto> options;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
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
        
        public List<OptionDto> getOptions() {
            return options;
        }
        
        public void setOptions(List<OptionDto> options) {
            this.options = options;
        }
    }
    
    public static class OptionDto {
        private Long id;
        private String value;
        private BigDecimal priceModifier;
        private Integer displayOrder;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
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
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public List<VariantDto> getVariants() {
        return variants;
    }
    
    public void setVariants(List<VariantDto> variants) {
        this.variants = variants;
    }
}
