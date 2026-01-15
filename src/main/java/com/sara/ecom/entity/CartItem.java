package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "product_image")
    private String productImage;
    
    // For DESIGNED products
    @Column(name = "design_id")
    private Long designId;
    
    @Column(name = "fabric_id")
    private Long fabricId;
    
    @Column(name = "fabric_price", precision = 10, scale = 2)
    private BigDecimal fabricPrice;
    
    @Column(name = "design_price", precision = 10, scale = 2)
    private BigDecimal designPrice;
    
    // Selected variants (JSON)
    @Column(columnDefinition = "TEXT")
    private String variants;
    
    // Custom form data for custom products (JSON)
    @Column(name = "custom_form_data", columnDefinition = "TEXT")
    private String customFormData;
    
    // For custom uploaded design
    @Column(name = "uploaded_design_url")
    private String uploadedDesignUrl;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProductType {
        PLAIN, DESIGNED, DIGITAL, CUSTOM
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }
    
    private void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public ProductType getProductType() {
        return productType;
    }
    
    public void setProductType(ProductType productType) {
        this.productType = productType;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public Long getDesignId() {
        return designId;
    }
    
    public void setDesignId(Long designId) {
        this.designId = designId;
    }
    
    public Long getFabricId() {
        return fabricId;
    }
    
    public void setFabricId(Long fabricId) {
        this.fabricId = fabricId;
    }
    
    public BigDecimal getFabricPrice() {
        return fabricPrice;
    }
    
    public void setFabricPrice(BigDecimal fabricPrice) {
        this.fabricPrice = fabricPrice;
    }
    
    public BigDecimal getDesignPrice() {
        return designPrice;
    }
    
    public void setDesignPrice(BigDecimal designPrice) {
        this.designPrice = designPrice;
    }
    
    public String getVariants() {
        return variants;
    }
    
    public void setVariants(String variants) {
        this.variants = variants;
    }
    
    public String getCustomFormData() {
        return customFormData;
    }
    
    public void setCustomFormData(String customFormData) {
        this.customFormData = customFormData;
    }
    
    public String getUploadedDesignUrl() {
        return uploadedDesignUrl;
    }
    
    public void setUploadedDesignUrl(String uploadedDesignUrl) {
        this.uploadedDesignUrl = uploadedDesignUrl;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateTotalPrice();
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotalPrice();
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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
}
