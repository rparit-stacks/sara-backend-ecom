package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
public class Coupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;
    
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal value;
    
    @Column(name = "min_order", precision = 10, scale = 2)
    private BigDecimal minOrder;
    
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;
    
    @Column(name = "usage_limit")
    private Integer usageLimit; // Global usage limit
    
    @Column(name = "per_user_usage_limit")
    private Integer perUserUsageLimit; // Per-user usage limit (null = unlimited)
    
    @Column(name = "used_count")
    private Integer usedCount = 0;
    
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum CouponType {
        PERCENTAGE, FIXED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public CouponType getType() {
        return type;
    }
    
    public void setType(CouponType type) {
        this.type = type;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public BigDecimal getMinOrder() {
        return minOrder;
    }
    
    public void setMinOrder(BigDecimal minOrder) {
        this.minOrder = minOrder;
    }
    
    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }
    
    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }
    
    public Integer getUsageLimit() {
        return usageLimit;
    }
    
    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }
    
    public Integer getPerUserUsageLimit() {
        return perUserUsageLimit;
    }
    
    public void setPerUserUsageLimit(Integer perUserUsageLimit) {
        this.perUserUsageLimit = perUserUsageLimit;
    }
    
    public Integer getUsedCount() {
        return usedCount;
    }
    
    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
    
    public LocalDateTime getValidFrom() {
        return validFrom;
    }
    
    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }
    
    public LocalDateTime getValidUntil() {
        return validUntil;
    }
    
    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void incrementUsedCount() {
        this.usedCount++;
    }
}
