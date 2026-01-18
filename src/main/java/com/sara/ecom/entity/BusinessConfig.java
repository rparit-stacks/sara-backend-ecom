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
    
    // Currency API Configuration
    @Column(name = "currency_api_key", length = 255)
    private String currencyApiKey;
    
    @Column(name = "currency_api_provider", length = 50)
    private String currencyApiProvider = "exchangerate-api";
    
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
}
