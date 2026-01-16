package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_product_config")
public class CustomProductConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "page_title")
    private String pageTitle;
    
    @Column(name = "page_description", columnDefinition = "TEXT")
    private String pageDescription;
    
    @Column(name = "upload_label")
    private String uploadLabel;
    
    @Column(name = "design_price", precision = 10, scale = 2)
    private BigDecimal designPrice;
    
    @Column(name = "min_quantity")
    private Integer minQuantity = 1;
    
    @Column(name = "max_quantity")
    private Integer maxQuantity = 100;
    
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;
    
    // UI Text Fields - All button labels and instructions
    @Column(name = "upload_button_text")
    private String uploadButtonText = "Choose Design File";
    
    @Column(name = "continue_button_text")
    private String continueButtonText = "Continue";
    
    @Column(name = "submit_button_text")
    private String submitButtonText = "Submit";
    
    @Column(name = "add_to_cart_button_text")
    private String addToCartButtonText = "Add to Cart";
    
    @Column(name = "select_fabric_label")
    private String selectFabricLabel = "Select Fabric";
    
    @Column(name = "quantity_label")
    private String quantityLabel = "Quantity";
    
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
    
    // Business Logic Fields
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;
    
    @Column(name = "hsn_code")
    private String hsnCode;
    
    // Recommended fabrics for custom products
    @ElementCollection
    @CollectionTable(name = "custom_config_recommended_fabrics", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "plain_product_id")
    private List<Long> recommendedFabricIds = new ArrayList<>();
    
    // Variants for custom products (from config)
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<CustomConfigVariant> variants = new ArrayList<>();
    
    // Pricing slabs for custom products (from config)
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, minQuantity ASC")
    private List<CustomConfigPricingSlab> pricingSlabs = new ArrayList<>();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
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
    
    public String getPageTitle() {
        return pageTitle;
    }
    
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }
    
    public String getPageDescription() {
        return pageDescription;
    }
    
    public void setPageDescription(String pageDescription) {
        this.pageDescription = pageDescription;
    }
    
    public String getUploadLabel() {
        return uploadLabel;
    }
    
    public void setUploadLabel(String uploadLabel) {
        this.uploadLabel = uploadLabel;
    }
    
    public BigDecimal getDesignPrice() {
        return designPrice;
    }
    
    public void setDesignPrice(BigDecimal designPrice) {
        this.designPrice = designPrice;
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
    
    public String getTermsAndConditions() {
        return termsAndConditions;
    }
    
    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // UI Text Fields Getters and Setters
    public String getUploadButtonText() {
        return uploadButtonText;
    }
    
    public void setUploadButtonText(String uploadButtonText) {
        this.uploadButtonText = uploadButtonText;
    }
    
    public String getContinueButtonText() {
        return continueButtonText;
    }
    
    public void setContinueButtonText(String continueButtonText) {
        this.continueButtonText = continueButtonText;
    }
    
    public String getSubmitButtonText() {
        return submitButtonText;
    }
    
    public void setSubmitButtonText(String submitButtonText) {
        this.submitButtonText = submitButtonText;
    }
    
    public String getAddToCartButtonText() {
        return addToCartButtonText;
    }
    
    public void setAddToCartButtonText(String addToCartButtonText) {
        this.addToCartButtonText = addToCartButtonText;
    }
    
    public String getSelectFabricLabel() {
        return selectFabricLabel;
    }
    
    public void setSelectFabricLabel(String selectFabricLabel) {
        this.selectFabricLabel = selectFabricLabel;
    }
    
    public String getQuantityLabel() {
        return quantityLabel;
    }
    
    public void setQuantityLabel(String quantityLabel) {
        this.quantityLabel = quantityLabel;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    // Business Logic Fields Getters and Setters
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
    
    public List<Long> getRecommendedFabricIds() {
        return recommendedFabricIds;
    }
    
    public void setRecommendedFabricIds(List<Long> recommendedFabricIds) {
        this.recommendedFabricIds = recommendedFabricIds;
    }
    
    public List<CustomConfigVariant> getVariants() {
        return variants;
    }
    
    public void setVariants(List<CustomConfigVariant> variants) {
        this.variants = variants;
    }
    
    public List<CustomConfigPricingSlab> getPricingSlabs() {
        return pricingSlabs;
    }
    
    public void setPricingSlabs(List<CustomConfigPricingSlab> pricingSlabs) {
        this.pricingSlabs = pricingSlabs;
    }
    
    public void addVariant(CustomConfigVariant variant) {
        variants.add(variant);
        variant.setConfig(this);
    }
    
    public void removeVariant(CustomConfigVariant variant) {
        variants.remove(variant);
        variant.setConfig(null);
    }
    
    public void addPricingSlab(CustomConfigPricingSlab slab) {
        pricingSlabs.add(slab);
        slab.setConfig(this);
    }
    
    public void removePricingSlab(CustomConfigPricingSlab slab) {
        pricingSlabs.remove(slab);
        slab.setConfig(null);
    }
}
