package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequest {
    private String name;
    private String type;
    private Long categoryId;
    private String description;
    private String status;
    private List<String> images; // Deprecated - use media instead
    private List<MediaRequest> media;
    private List<DetailSectionRequest> detailSections;
    private List<CustomFieldRequest> customFields;
    private List<VariantRequest> variants;
    
    // For DESIGNED products
    private BigDecimal designPrice;
    private Long designId;
    private List<Long> recommendedFabricIds;
    
    // For PLAIN products
    private Long plainProductId;
    
    // For DIGITAL products
    private BigDecimal price;
    private BigDecimal pricePerMeter;
    private String fileUrl;
    
    // Common display flags
    private Boolean isNew;
    private Boolean isSale;
    private BigDecimal originalPrice;
    private BigDecimal gstRate;
    
    public static class DetailSectionRequest {
        private String title;
        private String content;
        private Integer displayOrder;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Integer getDisplayOrder() {
            return displayOrder;
        }
        
        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }
    
    public static class MediaRequest {
        private String url;
        private String type; // "image" or "video"
        private Integer displayOrder;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Integer getDisplayOrder() {
            return displayOrder;
        }
        
        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }

    public static class CustomFieldRequest {
        private String label;
        private String fieldType;
        private String placeholder;
        private boolean isRequired;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public boolean isRequired() {
            return isRequired;
        }

        public void setRequired(boolean required) {
            isRequired = required;
        }
    }

    public static class VariantRequest {
        private String name;
        private String type;
        private String unit;
        private List<VariantOptionRequest> options;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public List<VariantOptionRequest> getOptions() {
            return options;
        }

        public void setOptions(List<VariantOptionRequest> options) {
            this.options = options;
        }
    }

    public static class VariantOptionRequest {
        private String value;
        private BigDecimal priceModifier;

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
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getImages() {
        return images;
    }
    
    public void setImages(List<String> images) {
        this.images = images;
    }
    
    public List<DetailSectionRequest> getDetailSections() {
        return detailSections;
    }
    
    public void setDetailSections(List<DetailSectionRequest> detailSections) {
        this.detailSections = detailSections;
    }

    public List<CustomFieldRequest> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<CustomFieldRequest> customFields) {
        this.customFields = customFields;
    }

    public List<VariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantRequest> variants) {
        this.variants = variants;
    }
    
    public BigDecimal getDesignPrice() {
        return designPrice;
    }
    
    public void setDesignPrice(BigDecimal designPrice) {
        this.designPrice = designPrice;
    }
    
    public Long getDesignId() {
        return designId;
    }
    
    public void setDesignId(Long designId) {
        this.designId = designId;
    }
    
    public List<MediaRequest> getMedia() {
        return media;
    }
    
    public void setMedia(List<MediaRequest> media) {
        this.media = media;
    }
    
    public List<Long> getRecommendedFabricIds() {
        return recommendedFabricIds;
    }
    
    public void setRecommendedFabricIds(List<Long> recommendedFabricIds) {
        this.recommendedFabricIds = recommendedFabricIds;
    }
    
    public Long getPlainProductId() {
        return plainProductId;
    }
    
    public void setPlainProductId(Long plainProductId) {
        this.plainProductId = plainProductId;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPricePerMeter() {
        return pricePerMeter;
    }

    public void setPricePerMeter(BigDecimal pricePerMeter) {
        this.pricePerMeter = pricePerMeter;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public Boolean getIsNew() {
        return isNew;
    }
    
    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }
    
    public Boolean getIsSale() {
        return isSale;
    }
    
    public void setIsSale(Boolean isSale) {
        this.isSale = isSale;
    }
    
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }

    public Boolean getSale() {
        return isSale;
    }

    public void setSale(Boolean sale) {
        isSale = sale;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }
}
