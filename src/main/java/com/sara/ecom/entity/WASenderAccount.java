package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wa_sender_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WASenderAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    @Column(name = "bearer_token", nullable = false, columnDefinition = "TEXT")
    private String bearerToken;
    
    @Column(name = "whatsapp_number", nullable = false)
    private String whatsappNumber;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;
    
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
}
