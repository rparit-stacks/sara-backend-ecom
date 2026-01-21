package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_notification_logs")
public class WhatsAppNotificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId; // Reference to order
    
    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber; // Recipient phone number
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message; // Sent message
    
    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus; // SENT, DELIVERED, FAILED
    
    @Column(name = "message_id", length = 100)
    private String messageId; // DoubleTick message ID
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Error if failed
    
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
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
