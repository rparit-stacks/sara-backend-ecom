package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductDto {
    private Long id;
    private String name;
    private String slug;
    private String type;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String status;
    private List<String> images;
    private List<MediaDto> media;
    private List<DetailSectionDto> detailSections;
    private List<CustomFieldDto> customFields;
    private List<VariantDto> variants;
    
    // For DESIGNED products
    private BigDecimal designPrice;
    private Long designId;
    private List<Long> recommendedFabricIds;
    private List<PlainProductDto> recommendedFabrics;
    private List<PricingSlabDto> pricingSlabs; // Quantity-based pricing slabs
    
    // For PLAIN products
    private Long plainProductId;
    private PlainProductDto plainProduct;
    
    // For DIGITAL products
    private BigDecimal price;
    private BigDecimal pricePerMeter;
    private String fileUrl;
    private Long sourceDesignProductId; // Link to Design Product if created from it
    
    // Common display flags
    private Boolean isNew;
    private Boolean isSale;
    private BigDecimal originalPrice;
    private BigDecimal gstRate;
    private String hsnCode;
    private LocalDateTime createdAt;
    
    public static class DetailSectionDto {
        private Long id;
        private String title;
        private String content;
        private Integer displayOrder;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
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
    
    public static class MediaDto {
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

    public static class CustomFieldDto {
        private Long id;
        private String label;
        private String fieldType;
        private String placeholder;
        private boolean isRequired;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

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

    public static class VariantDto {
        private Long id;
        private String name;
        private String type;
        private String unit;
        private Integer displayOrder;
        private List<VariantOptionDto> options;

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

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public List<VariantOptionDto> getOptions() {
            return options;
        }

        public void setOptions(List<VariantOptionDto> options) {
            this.options = options;
        }
    }

    public static class VariantOptionDto {
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
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
    
    public List<MediaDto> getMedia() {
        return media;
    }
    
    public void setMedia(List<MediaDto> media) {
        this.media = media;
    }
    
    public List<DetailSectionDto> getDetailSections() {
        return detailSections;
    }
    
    public void setDetailSections(List<DetailSectionDto> detailSections) {
        this.detailSections = detailSections;
    }

    public List<CustomFieldDto> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<CustomFieldDto> customFields) {
        this.customFields = customFields;
    }

    public List<VariantDto> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantDto> variants) {
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
    
    public List<Long> getRecommendedFabricIds() {
        return recommendedFabricIds;
    }
    
    public void setRecommendedFabricIds(List<Long> recommendedFabricIds) {
        this.recommendedFabricIds = recommendedFabricIds;
    }
    
    public List<PlainProductDto> getRecommendedFabrics() {
        return recommendedFabrics;
    }
    
    public void setRecommendedFabrics(List<PlainProductDto> recommendedFabrics) {
        this.recommendedFabrics = recommendedFabrics;
    }
    
    public Long getPlainProductId() {
        return plainProductId;
    }
    
    public void setPlainProductId(Long plainProductId) {
        this.plainProductId = plainProductId;
    }
    
    public PlainProductDto getPlainProduct() {
        return plainProduct;
    }
    
    public void setPlainProduct(PlainProductDto plainProduct) {
        this.plainProduct = plainProduct;
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
    
    public Long getSourceDesignProductId() {
        return sourceDesignProductId;
    }
    
    public void setSourceDesignProductId(Long sourceDesignProductId) {
        this.sourceDesignProductId = sourceDesignProductId;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }
    
    public BigDecimal getGstRate() {
        return gstRate;
    }
    
    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }
    
    public String getHsnCode() {
        return hsnCode;
    }
    
    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<PricingSlabDto> getPricingSlabs() {
        return pricingSlabs;
    }
    
    public void setPricingSlabs(List<PricingSlabDto> pricingSlabs) {
        this.pricingSlabs = pricingSlabs;
    }
    
    // Pricing Slab DTO inner class
    public static class PricingSlabDto {
        private Long id;
        private Integer minQuantity;
        private Integer maxQuantity;
        private String discountType; // "FIXED_AMOUNT" or "PERCENTAGE"
        private BigDecimal discountValue; // Discount amount (₹X for FIXED_AMOUNT, X% for PERCENTAGE)
        private Integer displayOrder;
        
        // Legacy field - kept for backward compatibility
        private BigDecimal pricePerMeter;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Integer getMinQuantity() {
            return minQuantity;
        }
        
        public void setMinQuantity(Integer minQuantity) {
            this.minQuantity = minQuantity;
        }
        
        public Integer getMaxQuantity() {
            return maxQuantity;
        }
        
        public void setMaxQuantity(Integer maxQuantity) {
            this.maxQuantity = maxQuantity;
        }
        
        public String getDiscountType() {
            return discountType;
        }
        
        public void setDiscountType(String discountType) {
            this.discountType = discountType;
        }
        
        public BigDecimal getDiscountValue() {
            return discountValue;
        }
        
        public void setDiscountValue(BigDecimal discountValue) {
            this.discountValue = discountValue;
        }
        
        public Integer getDisplayOrder() {
            return displayOrder;
        }
        
        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
        
        // Legacy getter/setter - kept for backward compatibility
        public BigDecimal getPricePerMeter() {
            return pricePerMeter;
        }
        
        public void setPricePerMeter(BigDecimal pricePerMeter) {
            this.pricePerMeter = pricePerMeter;
        }
    }
}
