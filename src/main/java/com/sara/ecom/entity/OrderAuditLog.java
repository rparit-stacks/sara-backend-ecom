package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_audit_log")
public class OrderAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;
    
    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // STATUS_UPDATE, PRICE_UPDATE, ITEM_UPDATE, ADDRESS_UPDATE, etc.
    
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getChangeReason() {
        return changeReason;
    }
    
    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
