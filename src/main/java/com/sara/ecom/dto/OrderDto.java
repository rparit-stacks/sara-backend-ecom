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
    private String customStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentId;
    private String notes;
    private String swipeInvoiceId;
    private String swipeInvoiceNumber;
    private String swipeIrn;
    private String swipeQrCode;
    private String swipeInvoiceUrl;
    private String invoiceStatus; // NOT_CREATED or CREATED
    private LocalDateTime invoiceCreatedAt;
    private LocalDateTime createdAt;
    // Payment currency and amount as charged/expected by gateway
    private String paymentCurrency;
    private BigDecimal paymentAmount;
    
    // Cancellation fields
    private String cancellationReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    
    // Refund fields
    private BigDecimal refundAmount;
    private LocalDateTime refundDate;
    private String refundTransactionId;
    private String refundReason;

    // Last Swipe invoice error for admin display (our_system vs swipe, message, hint)
    private String lastInvoiceErrorSource;
    private String lastInvoiceErrorMessage;
    private String lastInvoiceErrorHint;
    
    public static class OrderItemDto {
        private Long id;
        private String productType;
        private Long productId;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
        private BigDecimal gstRate;
        private BigDecimal gstAmount;
        private Map<String, String> variants; // Legacy format for backward compatibility
        private Map<String, VariantSelectionDto> variantSelections; // New structured format
        private List<VariantDisplayInfo> variantDisplay; // Resolved variant/option names for display
        private Map<String, Object> customData;
        /** Map of custom field id (as string) -> label for display in order dashboard */
        private Map<String, String> customFieldLabels;
        /** Map of custom field key -> "design" | "fabric" | "system" for splitting Design vs Fabric sections */
        private Map<String, String> customFieldSource;
        private Long designId;
        private Long fabricId;
        /** Resolved fabric product name when DESIGNED + fabricId present */
        private String fabricName;
        private String digitalDownloadUrl;
        private String zipPassword;
        private String uploadedDesignUrl;

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
        public BigDecimal getGstRate() { return gstRate; }
        public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }
        public BigDecimal getGstAmount() { return gstAmount; }
        public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }
        public Map<String, String> getVariants() { return variants; }
        public void setVariants(Map<String, String> variants) { this.variants = variants; }
        public Map<String, VariantSelectionDto> getVariantSelections() { return variantSelections; }
        public void setVariantSelections(Map<String, VariantSelectionDto> variantSelections) { this.variantSelections = variantSelections; }
        public List<VariantDisplayInfo> getVariantDisplay() { return variantDisplay; }
        public void setVariantDisplay(List<VariantDisplayInfo> variantDisplay) { this.variantDisplay = variantDisplay; }
        public Map<String, Object> getCustomData() { return customData; }
        public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
        public Map<String, String> getCustomFieldLabels() { return customFieldLabels; }
        public void setCustomFieldLabels(Map<String, String> customFieldLabels) { this.customFieldLabels = customFieldLabels; }
        public Map<String, String> getCustomFieldSource() { return customFieldSource; }
        public void setCustomFieldSource(Map<String, String> customFieldSource) { this.customFieldSource = customFieldSource; }
        public Long getDesignId() { return designId; }
        public void setDesignId(Long designId) { this.designId = designId; }
        public Long getFabricId() { return fabricId; }
        public void setFabricId(Long fabricId) { this.fabricId = fabricId; }
        public String getFabricName() { return fabricName; }
        public void setFabricName(String fabricName) { this.fabricName = fabricName; }
        public String getDigitalDownloadUrl() { return digitalDownloadUrl; }
        public void setDigitalDownloadUrl(String digitalDownloadUrl) { this.digitalDownloadUrl = digitalDownloadUrl; }
        public String getZipPassword() { return zipPassword; }
        public void setZipPassword(String zipPassword) { this.zipPassword = zipPassword; }
        public String getUploadedDesignUrl() { return uploadedDesignUrl; }
        public void setUploadedDesignUrl(String uploadedDesignUrl) { this.uploadedDesignUrl = uploadedDesignUrl; }
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
    public String getCustomStatus() { 
        return customStatus; 
    }




    public void setCustomStatus(String customStatus) { 
        this.customStatus = customStatus; 
    }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
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
    public String getPaymentCurrency() { return paymentCurrency; }
    public void setPaymentCurrency(String paymentCurrency) { this.paymentCurrency = paymentCurrency; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public LocalDateTime getRefundDate() { return refundDate; }
    public void setRefundDate(LocalDateTime refundDate) { this.refundDate = refundDate; }
    public String getRefundTransactionId() { return refundTransactionId; }
    public void setRefundTransactionId(String refundTransactionId) { this.refundTransactionId = refundTransactionId; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getLastInvoiceErrorSource() { return lastInvoiceErrorSource; }
    public void setLastInvoiceErrorSource(String lastInvoiceErrorSource) { this.lastInvoiceErrorSource = lastInvoiceErrorSource; }
    public String getLastInvoiceErrorMessage() { return lastInvoiceErrorMessage; }
    public void setLastInvoiceErrorMessage(String lastInvoiceErrorMessage) { this.lastInvoiceErrorMessage = lastInvoiceErrorMessage; }
    public String getLastInvoiceErrorHint() { return lastInvoiceErrorHint; }
    public void setLastInvoiceErrorHint(String lastInvoiceErrorHint) { this.lastInvoiceErrorHint = lastInvoiceErrorHint; }
}
