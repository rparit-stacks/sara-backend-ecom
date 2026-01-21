package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    private Long id;
    
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    @Column(name = "user_email")
    private String userEmail;
    
    @Column(name = "user_name")
    private String userName;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @Column(name = "shipping_address_id")
    private Long shippingAddressId;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress; // JSON representation
    
    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress; // JSON representation
    
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal gst;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal shipping;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal total;
    
    @Column(name = "coupon_code")
    private String couponCode;
    
    @Column(name = "coupon_discount", precision = 10, scale = 2)
    private BigDecimal couponDiscount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(name = "custom_status", length = 100)
    private String customStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "swipe_invoice_id")
    private String swipeInvoiceId;
    
    @Column(name = "swipe_invoice_number")
    private String swipeInvoiceNumber;
    
    @Column(name = "swipe_irn")
    private String swipeIrn;
    
    @Column(name = "swipe_qr_code", columnDefinition = "TEXT")
    private String swipeQrCode;
    
    @Column(name = "swipe_invoice_url", columnDefinition = "TEXT")
    private String swipeInvoiceUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false)
    private InvoiceStatus invoiceStatus = InvoiceStatus.NOT_CREATED;
    
    @Column(name = "invoice_created_at")
    private LocalDateTime invoiceCreatedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Currency in which payment was made (e.g. INR, USD)
    @Column(name = "payment_currency", length = 10)
    private String paymentCurrency;

    // Amount charged by the payment gateway in the original currency
    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;
    
    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
    
    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }
    
    public enum InvoiceStatus {
        NOT_CREATED, CREATED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderNumber == null) {
            // Use the id as orderNumber (id is already set as 7-digit random number)
            if (id != null) {
                orderNumber = String.valueOf(id);
            } else {
                // Fallback: generate random 7-digit number (1000000 to 9999999)
                long randomOrderId = 1000000L + (long)(Math.random() * 9000000L);
                orderNumber = String.valueOf(randomOrderId);
            }
        }
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
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public Long getShippingAddressId() {
        return shippingAddressId;
    }
    
    public void setShippingAddressId(Long shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getBillingAddress() {
        return billingAddress;
    }
    
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public BigDecimal getGst() {
        return gst;
    }
    
    public void setGst(BigDecimal gst) {
        this.gst = gst;
    }
    
    public BigDecimal getShipping() {
        return shipping;
    }
    
    public void setShipping(BigDecimal shipping) {
        this.shipping = shipping;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public String getCouponCode() {
        return couponCode;
    }
    
    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
    
    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }
    
    public void setCouponDiscount(BigDecimal couponDiscount) {
        this.couponDiscount = couponDiscount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public String getCustomStatus() {
        return customStatus;
    }
    
    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getSwipeInvoiceId() {
        return swipeInvoiceId;
    }
    
    public void setSwipeInvoiceId(String swipeInvoiceId) {
        this.swipeInvoiceId = swipeInvoiceId;
    }
    
    public String getSwipeInvoiceNumber() {
        return swipeInvoiceNumber;
    }
    
    public void setSwipeInvoiceNumber(String swipeInvoiceNumber) {
        this.swipeInvoiceNumber = swipeInvoiceNumber;
    }
    
    public String getSwipeIrn() {
        return swipeIrn;
    }
    
    public void setSwipeIrn(String swipeIrn) {
        this.swipeIrn = swipeIrn;
    }
    
    public String getSwipeQrCode() {
        return swipeQrCode;
    }
    
    public void setSwipeQrCode(String swipeQrCode) {
        this.swipeQrCode = swipeQrCode;
    }
    
    public String getSwipeInvoiceUrl() {
        return swipeInvoiceUrl;
    }
    
    public void setSwipeInvoiceUrl(String swipeInvoiceUrl) {
        this.swipeInvoiceUrl = swipeInvoiceUrl;
    }
    
    public InvoiceStatus getInvoiceStatus() {
        return invoiceStatus;
    }
    
    public void setInvoiceStatus(InvoiceStatus invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }
    
    public LocalDateTime getInvoiceCreatedAt() {
        return invoiceCreatedAt;
    }
    
    public void setInvoiceCreatedAt(LocalDateTime invoiceCreatedAt) {
        this.invoiceCreatedAt = invoiceCreatedAt;
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

    public String getPaymentCurrency() {
        return paymentCurrency;
    }

    public void setPaymentCurrency(String paymentCurrency) {
        this.paymentCurrency = paymentCurrency;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
