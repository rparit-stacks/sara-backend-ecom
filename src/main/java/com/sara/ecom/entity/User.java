package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @Column(unique = true, nullable = false)
    private String email;
    
    private String firstName;
    
    private String lastName;
    
    private String phoneNumber;
    
    // Legacy single address fields (kept for backward compatibility)
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;
    
    private String oauthProviderId;
    
    private Boolean emailVerified;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserAddress> addresses = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (emailVerified == null) {
            emailVerified = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum AuthProvider {
        GOOGLE, OTP
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE
    }
}
