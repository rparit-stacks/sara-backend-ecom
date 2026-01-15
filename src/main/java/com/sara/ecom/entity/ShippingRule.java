package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipping_rules")
public class ShippingRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_name")
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private Scope scope;
    
    @Column(name = "state")
    private String state; // Required if scope is STATE_WISE
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false)
    private CalculationType calculationType;
    
    @Column(name = "flat_price", precision = 10, scale = 2)
    private BigDecimal flatPrice; // Required if calculationType is FLAT
    
    @Column(name = "free_shipping_above", precision = 10, scale = 2)
    private BigDecimal freeShippingAbove; // Optional: Free shipping above this amount
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "priority")
    private Integer priority = 0; // Higher priority = checked first
    
    @OneToMany(mappedBy = "shippingRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingRange> ranges = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Scope {
        ALL_INDIA, STATE_WISE
    }
    
    public enum CalculationType {
        FLAT, RANGE_BASED
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
    
    // Helper methods
    public void addRange(ShippingRange range) {
        ranges.add(range);
        range.setShippingRule(this);
    }
    
    public void removeRange(ShippingRange range) {
        ranges.remove(range);
        range.setShippingRule(null);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public void setScope(Scope scope) {
        this.scope = scope;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public CalculationType getCalculationType() {
        return calculationType;
    }
    
    public void setCalculationType(CalculationType calculationType) {
        this.calculationType = calculationType;
    }
    
    public BigDecimal getFlatPrice() {
        return flatPrice;
    }
    
    public void setFlatPrice(BigDecimal flatPrice) {
        this.flatPrice = flatPrice;
    }
    
    public BigDecimal getFreeShippingAbove() {
        return freeShippingAbove;
    }
    
    public void setFreeShippingAbove(BigDecimal freeShippingAbove) {
        this.freeShippingAbove = freeShippingAbove;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public List<ShippingRange> getRanges() {
        return ranges;
    }
    
    public void setRanges(List<ShippingRange> ranges) {
        this.ranges = ranges;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
