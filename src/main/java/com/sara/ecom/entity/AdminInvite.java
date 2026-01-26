package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_invites")
public class AdminInvite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false, unique = true, length = 64)
    private String token;
    
    @Column(name = "invited_by")
    private String invitedBy; // Email of admin who sent the invite
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    public enum InviteStatus {
        PENDING, ACCEPTED, EXPIRED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (token == null) {
            token = UUID.randomUUID().toString().replace("-", "");
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(7); // 7 days expiry
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getInvitedBy() {
        return invitedBy;
    }
    
    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }
    
    public InviteStatus getStatus() {
        return status;
    }
    
    public void setStatus(InviteStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
    
    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
