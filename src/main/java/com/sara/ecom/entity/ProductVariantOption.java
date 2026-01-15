package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variant_options")
public class ProductVariantOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;
    
    @Column(nullable = false)
    private String value; // e.g., "Small", "Red"
    
    @Column(name = "frontend_id")
    private String frontendId; // To link with combinations
    
    @Column(name = "price_modifier", precision = 10, scale = 2)
    private BigDecimal priceModifier = BigDecimal.ZERO;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ProductVariant getVariant() {
        return variant;
    }
    
    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }

    public String getFrontendId() {
        return frontendId;
    }

    public void setFrontendId(String frontendId) {
        this.frontendId = frontendId;
    }
    
    public BigDecimal getPriceModifier() {
        return priceModifier;
    }
    
    public void setPriceModifier(BigDecimal priceModifier) {
        this.priceModifier = priceModifier;
    }
}
