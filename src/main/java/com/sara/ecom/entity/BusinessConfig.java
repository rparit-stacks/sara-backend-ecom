package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_config")
public class BusinessConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "business_gstin", length = 15)
    private String businessGstin;
    
    @Column(name = "business_name")
    private String businessName;
    
    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;
    
    @Column(name = "business_state")
    private String businessState;
    
    @Column(name = "business_city")
    private String businessCity;
    
    @Column(name = "business_pincode")
    private String businessPincode;
    
    @Column(name = "business_phone")
    private String businessPhone;
    
    @Column(name = "business_email")
    private String businessEmail;
    
    @Column(name = "swipe_api_key", columnDefinition = "TEXT")
    private String swipeApiKey;
    
    @Column(name = "swipe_enabled")
    private Boolean swipeEnabled = false;
    
    @Column(name = "einvoice_enabled")
    private Boolean einvoiceEnabled = false;
    
    // Payment Gateway Configuration
    @Column(name = "razorpay_key_id", length = 255)
    private String razorpayKeyId;
    
    @Column(name = "razorpay_key_secret", columnDefinition = "TEXT")
    private String razorpayKeySecret;
    
    @Column(name = "razorpay_enabled")
    private Boolean razorpayEnabled = false;
    
    @Column(name = "stripe_public_key", length = 255)
    private String stripePublicKey;
    
    @Column(name = "stripe_secret_key", columnDefinition = "TEXT")
    private String stripeSecretKey;
    
    @Column(name = "stripe_enabled")
    private Boolean stripeEnabled = false;
    
    // Payment Mode Configuration
    @Column(name = "payment_mode", length = 50)
    private String paymentMode; // "FULL_COD", "PARTIAL_COD", "ONLINE_PAYMENT"
    
    @Column(name = "partial_cod_advance_percentage")
    private Integer partialCodAdvancePercentage; // 10-90
    
    @Column(name = "cod_enabled")
    private Boolean codEnabled = false;
    
    @Column(name = "partial_cod_enabled")
    private Boolean partialCodEnabled = false;
    
    @Column(name = "online_payment_enabled")
    private Boolean onlinePaymentEnabled = true;
    
    // Currency API Configuration
    @Column(name = "currency_api_key", length = 255)
    private String currencyApiKey;
    
    @Column(name = "currency_api_provider", length = 50)
    private String currencyApiProvider = "exchangerate-api";
    
    // DoubleTick WhatsApp Configuration
    @Column(name = "doubletick_api_key", columnDefinition = "TEXT")
    private String doubletickApiKey;
    
    @Column(name = "doubletick_sender_number", length = 50)
    private String doubletickSenderNumber;
    
    @Column(name = "doubletick_template_name", length = 100)
    private String doubletickTemplateName; // Template name for DoubleTick (e.g., "order_status_update")
    
    @Column(name = "doubletick_enabled")
    private Boolean doubletickEnabled = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
    
    public String getBusinessGstin() {
        return businessGstin;
    }
    
    public void setBusinessGstin(String businessGstin) {
        this.businessGstin = businessGstin;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getBusinessAddress() {
        return businessAddress;
    }
    
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }
    
    public String getBusinessState() {
        return businessState;
    }
    
    public void setBusinessState(String businessState) {
        this.businessState = businessState;
    }
    
    public String getBusinessCity() {
        return businessCity;
    }
    
    public void setBusinessCity(String businessCity) {
        this.businessCity = businessCity;
    }
    
    public String getBusinessPincode() {
        return businessPincode;
    }
    
    public void setBusinessPincode(String businessPincode) {
        this.businessPincode = businessPincode;
    }
    
    public String getBusinessPhone() {
        return businessPhone;
    }
    
    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }
    
    public String getBusinessEmail() {
        return businessEmail;
    }
    
    public void setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
    }
    
    public String getSwipeApiKey() {
        return swipeApiKey;
    }
    
    public void setSwipeApiKey(String swipeApiKey) {
        this.swipeApiKey = swipeApiKey;
    }
    
    public Boolean getSwipeEnabled() {
        return swipeEnabled;
    }
    
    public void setSwipeEnabled(Boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }
    
    public Boolean getEinvoiceEnabled() {
        return einvoiceEnabled;
    }
    
    public void setEinvoiceEnabled(Boolean einvoiceEnabled) {
        this.einvoiceEnabled = einvoiceEnabled;
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
    
    // Payment Gateway Getters and Setters
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
    
    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }
    
    public String getRazorpayKeySecret() {
        return razorpayKeySecret;
    }
    
    public void setRazorpayKeySecret(String razorpayKeySecret) {
        this.razorpayKeySecret = razorpayKeySecret;
    }
    
    public Boolean getRazorpayEnabled() {
        return razorpayEnabled;
    }
    
    public void setRazorpayEnabled(Boolean razorpayEnabled) {
        this.razorpayEnabled = razorpayEnabled;
    }
    
    public String getStripePublicKey() {
        return stripePublicKey;
    }
    
    public void setStripePublicKey(String stripePublicKey) {
        this.stripePublicKey = stripePublicKey;
    }
    
    public String getStripeSecretKey() {
        return stripeSecretKey;
    }
    
    public void setStripeSecretKey(String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
    }
    
    public Boolean getStripeEnabled() {
        return stripeEnabled;
    }
    
    public void setStripeEnabled(Boolean stripeEnabled) {
        this.stripeEnabled = stripeEnabled;
    }
    
    // Currency API Getters and Setters
    public String getCurrencyApiKey() {
        return currencyApiKey;
    }
    
    public void setCurrencyApiKey(String currencyApiKey) {
        this.currencyApiKey = currencyApiKey;
    }
    
    public String getCurrencyApiProvider() {
        return currencyApiProvider;
    }
    
    public void setCurrencyApiProvider(String currencyApiProvider) {
        this.currencyApiProvider = currencyApiProvider;
    }
    
    // DoubleTick WhatsApp Getters and Setters
    public String getDoubletickApiKey() {
        return doubletickApiKey;
    }
    
    public void setDoubletickApiKey(String doubletickApiKey) {
        this.doubletickApiKey = doubletickApiKey;
    }
    
    public String getDoubletickSenderNumber() {
        return doubletickSenderNumber;
    }
    
    public void setDoubletickSenderNumber(String doubletickSenderNumber) {
        this.doubletickSenderNumber = doubletickSenderNumber;
    }
    
    public Boolean getDoubletickEnabled() {
        return doubletickEnabled;
    }
    
    public void setDoubletickEnabled(Boolean doubletickEnabled) {
        this.doubletickEnabled = doubletickEnabled;
    }
    
    public String getDoubletickTemplateName() {
        return doubletickTemplateName;
    }
    
    public void setDoubletickTemplateName(String doubletickTemplateName) {
        this.doubletickTemplateName = doubletickTemplateName;
    }
    
    // Payment Mode Getters and Setters
    public String getPaymentMode() {
        return paymentMode;
    }
    
    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }
    
    public Integer getPartialCodAdvancePercentage() {
        return partialCodAdvancePercentage;
    }
    
    public void setPartialCodAdvancePercentage(Integer partialCodAdvancePercentage) {
        this.partialCodAdvancePercentage = partialCodAdvancePercentage;
    }
    
    public Boolean getCodEnabled() {
        return codEnabled;
    }
    
    public void setCodEnabled(Boolean codEnabled) {
        this.codEnabled = codEnabled;
    }
    
    public Boolean getPartialCodEnabled() {
        return partialCodEnabled;
    }
    
    public void setPartialCodEnabled(Boolean partialCodEnabled) {
        this.partialCodEnabled = partialCodEnabled;
    }
    
    public Boolean getOnlinePaymentEnabled() {
        return onlinePaymentEnabled;
    }
    
    public void setOnlinePaymentEnabled(Boolean onlinePaymentEnabled) {
        this.onlinePaymentEnabled = onlinePaymentEnabled;
    }
}
