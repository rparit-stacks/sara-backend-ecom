package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.Map;

public class AddToCartRequest {
    private String productType;
    private Long productId;
    private String productName;
    private String productImage;
    private Long designId;
    private Long fabricId;
    private BigDecimal fabricPrice;
    private BigDecimal designPrice;
    private Map<String, String> variants;
    private Map<String, Object> customFormData;
    private String uploadedDesignUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    
    // Getters and Setters
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }
    public Long getFabricId() { return fabricId; }
    public void setFabricId(Long fabricId) { this.fabricId = fabricId; }
    public BigDecimal getFabricPrice() { return fabricPrice; }
    public void setFabricPrice(BigDecimal fabricPrice) { this.fabricPrice = fabricPrice; }
    public BigDecimal getDesignPrice() { return designPrice; }
    public void setDesignPrice(BigDecimal designPrice) { this.designPrice = designPrice; }
    public Map<String, String> getVariants() { return variants; }
    public void setVariants(Map<String, String> variants) { this.variants = variants; }
    public Map<String, Object> getCustomFormData() { return customFormData; }
    public void setCustomFormData(Map<String, Object> customFormData) { this.customFormData = customFormData; }
    public String getUploadedDesignUrl() { return uploadedDesignUrl; }
    public void setUploadedDesignUrl(String uploadedDesignUrl) { this.uploadedDesignUrl = uploadedDesignUrl; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
