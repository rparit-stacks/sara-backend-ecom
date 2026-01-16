package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "custom_config_pricing_slabs")
public class CustomConfigPricingSlab {
    
    public enum DiscountType {
        FIXED_AMOUNT,  // Fixed discount per meter
        PERCENTAGE     // Percentage discount
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private CustomProductConfig config;
    
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;
    
    @Column(name = "max_quantity")
    private Integer maxQuantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;
    
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public CustomProductConfig getConfig() {
        return config;
    }
    
    public void setConfig(CustomProductConfig config) {
        this.config = config;
    }
    
    public Integer getMinQuantity() {
        return minQuantity;
    }
    
    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }
    
    public Integer getMaxQuantity() {
        return maxQuantity;
    }
    
    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }
    
    public DiscountType getDiscountType() {
        return discountType;
    }
    
    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }
    
    public BigDecimal getDiscountValue() {
        return discountValue;
    }
    
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
