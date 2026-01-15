package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String slug;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;
    
    @Column(name = "category_id")
    private Long categoryId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
    
    // For DESIGNED products
    @Column(name = "design_price", precision = 10, scale = 2)
    private BigDecimal designPrice;
    
    @Column(name = "design_id")
    private Long designId;
    
    // For PLAIN products - link to PlainProduct
    @Column(name = "plain_product_id")
    private Long plainProductId;
    
    // For DIGITAL products
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "file_url")
    private String fileUrl;
    
    // Link to source Design Product if this Digital Product was created from a Design Product
    @Column(name = "source_design_product_id")
    private Long sourceDesignProductId;
    
    @Column(name = "is_new")
    private Boolean isNew = false;
    
    @Column(name = "is_sale")
    private Boolean isSale = false;
    
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;
    
    // Getters and Setters
    public BigDecimal getGstRate() {
        return gstRate;
    }
    
    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductDetailSection> detailSections = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCustomField> customFields = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantCombination> combinations = new ArrayList<>();
    
    // For DESIGNED products - recommended fabrics
    @ElementCollection
    @CollectionTable(name = "product_recommended_fabrics", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "plain_product_id")
    private List<Long> recommendedFabricIds = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProductType {
        PLAIN, DESIGNED, DIGITAL
    }
    
    public enum Status {
        ACTIVE, INACTIVE
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public ProductType getType() {
        return type;
    }
    
    public void setType(ProductType type) {
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
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
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
    
    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }
    
    public List<ProductImage> getImages() {
        return images;
    }
    
    public void setImages(List<ProductImage> images) {
        this.images = images;
    }
    
    public List<ProductDetailSection> getDetailSections() {
        return detailSections;
    }
    
    public void setDetailSections(List<ProductDetailSection> detailSections) {
        this.detailSections = detailSections;
    }

    public List<ProductCustomField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<ProductCustomField> customFields) {
        this.customFields = customFields;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }

    public List<ProductVariantCombination> getCombinations() {
        return combinations;
    }

    public void setCombinations(List<ProductVariantCombination> combinations) {
        this.combinations = combinations;
    }
    
    public List<Long> getRecommendedFabricIds() {
        return recommendedFabricIds;
    }
    
    public void setRecommendedFabricIds(List<Long> recommendedFabricIds) {
        this.recommendedFabricIds = recommendedFabricIds;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }
    
    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
    
    public void addDetailSection(ProductDetailSection section) {
        detailSections.add(section);
        section.setProduct(this);
    }
    
    public void removeDetailSection(ProductDetailSection section) {
        detailSections.remove(section);
        section.setProduct(null);
    }

    public void addCustomField(ProductCustomField field) {
        customFields.add(field);
        field.setProduct(this);
    }

    public void removeCustomField(ProductCustomField field) {
        customFields.remove(field);
        field.setProduct(null);
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

    public void addCombination(ProductVariantCombination combination) {
        combinations.add(combination);
        combination.setProduct(this);
    }

    public void removeCombination(ProductVariantCombination combination) {
        combinations.remove(combination);
        combination.setProduct(null);
    }
}
