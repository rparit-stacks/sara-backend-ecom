package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ShippingRuleDto {
    private Long id;
    private String ruleName;
    private String scope; // ALL_INDIA, STATE_WISE
    private String state;
    private String calculationType; // FLAT, RANGE_BASED
    private BigDecimal flatPrice;
    private BigDecimal freeShippingAbove;
    private Boolean isActive;
    private Integer priority;
    private List<ShippingRangeDto> ranges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static class ShippingRangeDto {
        private Long id;
        private BigDecimal minCartValue;
        private BigDecimal maxCartValue;
        private BigDecimal shippingPrice;
        private Integer displayOrder;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public BigDecimal getMinCartValue() { return minCartValue; }
        public void setMinCartValue(BigDecimal minCartValue) { this.minCartValue = minCartValue; }
        public BigDecimal getMaxCartValue() { return maxCartValue; }
        public void setMaxCartValue(BigDecimal maxCartValue) { this.maxCartValue = maxCartValue; }
        public BigDecimal getShippingPrice() { return shippingPrice; }
        public void setShippingPrice(BigDecimal shippingPrice) { this.shippingPrice = shippingPrice; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public BigDecimal getFlatPrice() { return flatPrice; }
    public void setFlatPrice(BigDecimal flatPrice) { this.flatPrice = flatPrice; }
    public BigDecimal getFreeShippingAbove() { return freeShippingAbove; }
    public void setFreeShippingAbove(BigDecimal freeShippingAbove) { this.freeShippingAbove = freeShippingAbove; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public List<ShippingRangeDto> getRanges() { return ranges; }
    public void setRanges(List<ShippingRangeDto> ranges) { this.ranges = ranges; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
