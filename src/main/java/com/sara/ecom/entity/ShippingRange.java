package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shipping_ranges")
public class ShippingRange {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_rule_id", nullable = false)
    private ShippingRule shippingRule;
    
    @Column(name = "min_cart_value", precision = 10, scale = 2)
    private BigDecimal minCartValue; // null means 0
    
    @Column(name = "max_cart_value", precision = 10, scale = 2)
    private BigDecimal maxCartValue; // null means unlimited
    
    @Column(name = "shipping_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal shippingPrice;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ShippingRule getShippingRule() {
        return shippingRule;
    }
    
    public void setShippingRule(ShippingRule shippingRule) {
        this.shippingRule = shippingRule;
    }
    
    public BigDecimal getMinCartValue() {
        return minCartValue;
    }
    
    public void setMinCartValue(BigDecimal minCartValue) {
        this.minCartValue = minCartValue;
    }
    
    public BigDecimal getMaxCartValue() {
        return maxCartValue;
    }
    
    public void setMaxCartValue(BigDecimal maxCartValue) {
        this.maxCartValue = maxCartValue;
    }
    
    public BigDecimal getShippingPrice() {
        return shippingPrice;
    }
    
    public void setShippingPrice(BigDecimal shippingPrice) {
        this.shippingPrice = shippingPrice;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    // Helper method to check if cart value falls in this range
    public boolean matches(BigDecimal cartValue) {
        boolean minMatch = minCartValue == null || cartValue.compareTo(minCartValue) >= 0;
        boolean maxMatch = maxCartValue == null || cartValue.compareTo(maxCartValue) < 0;
        return minMatch && maxMatch;
    }
}
