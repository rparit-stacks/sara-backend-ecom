package com.sara.ecom.dto;

public class WhatsAppConfigDto {
    private String doubletickApiKey;
    private String doubletickSenderNumber;
    private String doubletickTemplateName;
    private Boolean doubletickEnabled;
    
    // Getters and Setters
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
