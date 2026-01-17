package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;
    
    @Column(name = "default_fallback_reply", columnDefinition = "TEXT")
    private String defaultFallbackReply;
    
    @Column(name = "webhook_secret", columnDefinition = "TEXT")
    private String webhookSecret; // WASender webhook secret for signature verification
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
