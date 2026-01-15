package com.sara.ecom.dto;

import java.time.LocalDateTime;

public class CustomDesignRequestDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String designType;
    private String description;
    private String referenceImage;
    private String status;
    private String adminNotes;
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDesignType() { return designType; }
    public void setDesignType(String designType) { this.designType = designType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceImage() { return referenceImage; }
    public void setReferenceImage(String referenceImage) { this.referenceImage = referenceImage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

class CustomDesignSubmitRequest {
    private String fullName;
    private String email;
    private String phone;
    private String designType;
    private String description;
    private String referenceImage;
    
    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDesignType() { return designType; }
    public void setDesignType(String designType) { this.designType = designType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceImage() { return referenceImage; }
    public void setReferenceImage(String referenceImage) { this.referenceImage = referenceImage; }
}
