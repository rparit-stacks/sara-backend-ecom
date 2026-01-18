package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OrderDto {
    private Long id;
    private String orderNumber;
    private String userEmail;
    private String userName;
    private List<OrderItemDto> items;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private BigDecimal subtotal;
    private BigDecimal gst;
    private BigDecimal shipping;
    private BigDecimal total;
    private String couponCode;
    private BigDecimal couponDiscount;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String notes;
    private String swipeInvoiceId;
    private String swipeInvoiceNumber;
    private String swipeIrn;
    private String swipeQrCode;
    private String swipeInvoiceUrl;
    private String invoiceStatus; // NOT_CREATED or CREATED
    private LocalDateTime invoiceCreatedAt;
    private LocalDateTime createdAt;
    
    public static class OrderItemDto {
        private Long id;
        private String productType;
        private Long productId;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
        private Map<String, String> variants;
        private Map<String, Object> customData;
        private Long designId;
        private Long fabricId;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public Map<String, String> getVariants() { return variants; }
        public void setVariants(Map<String, String> variants) { this.variants = variants; }
        public Map<String, Object> getCustomData() { return customData; }
        public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
        public Long getDesignId() { return designId; }
        public void setDesignId(Long designId) { this.designId = designId; }
        public Long getFabricId() { return fabricId; }
        public void setFabricId(Long fabricId) { this.fabricId = fabricId; }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
    public Map<String, Object> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }
    public Map<String, Object> getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Map<String, Object> billingAddress) { this.billingAddress = billingAddress; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getSwipeInvoiceId() { return swipeInvoiceId; }
    public void setSwipeInvoiceId(String swipeInvoiceId) { this.swipeInvoiceId = swipeInvoiceId; }
    public String getSwipeInvoiceNumber() { return swipeInvoiceNumber; }
    public void setSwipeInvoiceNumber(String swipeInvoiceNumber) { this.swipeInvoiceNumber = swipeInvoiceNumber; }
    public String getSwipeIrn() { return swipeIrn; }
    public void setSwipeIrn(String swipeIrn) { this.swipeIrn = swipeIrn; }
    public String getSwipeQrCode() { return swipeQrCode; }
    public void setSwipeQrCode(String swipeQrCode) { this.swipeQrCode = swipeQrCode; }
    public String getSwipeInvoiceUrl() { return swipeInvoiceUrl; }
    public void setSwipeInvoiceUrl(String swipeInvoiceUrl) { this.swipeInvoiceUrl = swipeInvoiceUrl; }
    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public LocalDateTime getInvoiceCreatedAt() { return invoiceCreatedAt; }
    public void setInvoiceCreatedAt(LocalDateTime invoiceCreatedAt) { this.invoiceCreatedAt = invoiceCreatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
