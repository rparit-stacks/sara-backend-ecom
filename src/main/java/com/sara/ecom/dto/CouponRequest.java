package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponRequest {
    private String code;
    private String type;
    private BigDecimal value;
    private BigDecimal minOrder;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private Integer perUserUsageLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private String applicability; // GLOBAL or USER_SPECIFIC
    private String allowedUserEmail; // Required when applicability is USER_SPECIFIC
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMinOrder() { return minOrder; }
    public void setMinOrder(BigDecimal minOrder) { this.minOrder = minOrder; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getPerUserUsageLimit() { return perUserUsageLimit; }
    public void setPerUserUsageLimit(Integer perUserUsageLimit) { this.perUserUsageLimit = perUserUsageLimit; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getApplicability() { return applicability; }
    public void setApplicability(String applicability) { this.applicability = applicability; }
    public String getAllowedUserEmail() { return allowedUserEmail; }
    public void setAllowedUserEmail(String allowedUserEmail) { this.allowedUserEmail = allowedUserEmail; }
}
