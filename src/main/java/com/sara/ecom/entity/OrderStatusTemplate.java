package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status_type", nullable = false, unique = true)
    private StatusType statusType;
    
    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum StatusType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        ORDER_SHIPPED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        REFUND_INITIATED,
        REFUND_COMPLETED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
