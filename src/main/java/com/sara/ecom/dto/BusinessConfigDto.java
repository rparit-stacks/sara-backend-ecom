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
    
    // Currency API Configuration
    private String currencyApiKey; // Include in POST/PUT, exclude in GET
    private String currencyApiProvider;
    
    // DoubleTick WhatsApp Configuration
    private String doubletickApiKey; // Include in POST/PUT, exclude in GET
    private String doubletickSenderNumber;
    private String doubletickTemplateName;
    private Boolean doubletickEnabled;
    
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
    
}
