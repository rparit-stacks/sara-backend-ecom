package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_config")
public class PaymentConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Razorpay Configuration
    @Column(name = "razorpay_key_id", length = 255)
    private String razorpayKeyId;
    
    @Column(name = "razorpay_key_secret", columnDefinition = "TEXT")
    private String razorpayKeySecret;
    
    @Column(name = "razorpay_enabled")
    private Boolean razorpayEnabled = false;
    
    // Stripe Configuration
    @Column(name = "stripe_public_key", length = 255)
    private String stripePublicKey;
    
    @Column(name = "stripe_secret_key", columnDefinition = "TEXT")
    private String stripeSecretKey;
    
    @Column(name = "stripe_enabled")
    private Boolean stripeEnabled = false;
    
    // COD Configuration
    @Column(name = "cod_enabled")
    private Boolean codEnabled = false;
    
    @Column(name = "partial_cod_enabled")
    private Boolean partialCodEnabled = false;
    
    @Column(name = "partial_cod_advance_percentage")
    private Integer partialCodAdvancePercentage; // 10-90
    
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
}
