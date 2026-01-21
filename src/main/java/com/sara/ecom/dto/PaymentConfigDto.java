package com.sara.ecom.dto;

public class PaymentConfigDto {
    private Long id;
    
    // Razorpay Configuration
    private String razorpayKeyId;
    private String razorpayKeySecret;
    private Boolean razorpayEnabled;
    
    // Stripe Configuration
    private String stripePublicKey;
    private String stripeSecretKey;
    private Boolean stripeEnabled;
    
    // COD Configuration
    private Boolean codEnabled;
    private Boolean partialCodEnabled;
    private Integer partialCodAdvancePercentage;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Integer getPartialCodAdvancePercentage() {
        return partialCodAdvancePercentage;
    }
    
    public void setPartialCodAdvancePercentage(Integer partialCodAdvancePercentage) {
        this.partialCodAdvancePercentage = partialCodAdvancePercentage;
    }
}
