package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sent_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "recipient_number", nullable = false)
    private String recipientNumber;
    
    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;
    
    @Column(name = "wasender_response", columnDefinition = "TEXT")
    private String wasenderResponse; // JSON response from WASender API
    
    @Column(name = "order_id")
    private Long orderId; // If related to order
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum MessageType {
        ORDER_NOTIFICATION,
        MANUAL,
        BROADCAST,
        CHATBOT_REPLY
    }
    
    public enum Status {
        PENDING,
        SENT,
        FAILED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == Status.SENT && sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (status == Status.SENT && sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
