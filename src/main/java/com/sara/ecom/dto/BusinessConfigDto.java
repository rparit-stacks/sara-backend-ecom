package com.sara.ecom.dto;

public class BusinessConfigDto {
    private Long id;
    private String businessGstin;
    private String businessName;
    private String businessAddress;
    private String businessState;
    private String businessCity;
    private String businessPincode;
    private String businessPhone;
    private String businessEmail;
    private String swipeApiKey; // Include in POST/PUT, exclude in GET
    private Boolean swipeEnabled;
    private Boolean einvoiceEnabled;
    
    // Payment Gateway Configuration
    private String razorpayKeyId;
    private String razorpayKeySecret; // Include in POST/PUT, exclude in GET
    private Boolean razorpayEnabled;
    private String stripePublicKey;
    private String stripeSecretKey; // Include in POST/PUT, exclude in GET
    private Boolean stripeEnabled;
    
    // Currency API Configuration
    private String currencyApiKey; // Include in POST/PUT, exclude in GET
    private String currencyApiProvider;
    
    // DoubleTick WhatsApp Configuration
    private String doubletickApiKey; // Include in POST/PUT, exclude in GET
    private String doubletickSenderNumber;
    private String doubletickTemplateName;
    private Boolean doubletickEnabled;
    
    // Payment Mode Configuration
    private String paymentMode; // "FULL_COD", "PARTIAL_COD", "ONLINE_PAYMENT"
    private Integer partialCodAdvancePercentage; // 10-90
    private Boolean codEnabled;
    private Boolean partialCodEnabled;
    private Boolean onlinePaymentEnabled;
    
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
