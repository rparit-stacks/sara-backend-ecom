package com.sara.ecom.dto;

import java.time.LocalDateTime;

public class WishlistDto {
    private Long id;
    private String productType;
    private Long productId;
    private LocalDateTime createdAt;
    
    // Product details (populated from product service)
    private String productName;
    private String productImage;
    private String productPrice;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getProductPrice() { return productPrice; }
    public void setProductPrice(String productPrice) { this.productPrice = productPrice; }
}
