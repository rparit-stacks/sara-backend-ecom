package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CartDto {
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal gst;
    private BigDecimal shipping;
    private BigDecimal couponDiscount;
    private String appliedCouponCode;
    private BigDecimal total;
    private Integer itemCount;
    
    public static class CartItemDto {
        private Long id;
        private String productType;
        private Long productId;
        private String productSlug; // For custom products with slug-based URLs
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
        private BigDecimal totalPrice;
        private BigDecimal gstRate;
        private BigDecimal gstAmount;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductSlug() { return productSlug; }
        public void setProductSlug(String productSlug) { this.productSlug = productSlug; }
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
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public BigDecimal getGstRate() { return gstRate; }
        public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }
        public BigDecimal getGstAmount() { return gstAmount; }
        public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }
    }
    
    // Getters and Setters
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }
    public String getAppliedCouponCode() { return appliedCouponCode; }
    public void setAppliedCouponCode(String appliedCouponCode) { this.appliedCouponCode = appliedCouponCode; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
}
