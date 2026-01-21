package com.sara.ecom.dto;

public class WhatsAppTemplateDto {
    private Long id;
    private String statusType;
    private String templateName;
    private String messageTemplate;
    private Boolean isEnabled;
    private String createdAt;
    private String updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStatusType() {
        return statusType;
    }
    
    public void setStatusType(String statusType) {
        this.statusType = statusType;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public String getMessageTemplate() {
        return messageTemplate;
    }
    
    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }
    
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
