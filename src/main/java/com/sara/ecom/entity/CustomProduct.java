package com.sara.ecom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Column(name = "is_saved", nullable = false)
    @Builder.Default
    private Boolean isSaved = false;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "design_url", columnDefinition = "TEXT")
    private String designUrl;
    
    // Generated mockup URLs (JSON array stored as TEXT)
    @Column(name = "mockup_urls", columnDefinition = "TEXT")
    private String mockupUrls;
    
    @Column(name = "fabric_id")
    private Long fabricId;
    
    @Column(name = "fabric_price", precision = 10, scale = 2)
    private java.math.BigDecimal fabricPrice;
    
    @Column(name = "design_price", precision = 10, scale = 2)
    private java.math.BigDecimal designPrice;
    
    // Selected variants (JSON)
    @Column(columnDefinition = "TEXT")
    private String variants;
    
    // Custom form data (JSON)
    @Column(name = "custom_form_data", columnDefinition = "TEXT")
    private String customFormData;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private java.math.BigDecimal totalPrice;
    
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
