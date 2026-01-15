package com.sara.ecom.dto;

import java.util.Map;

public class CreateOrderRequest {
    private Long shippingAddressId;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private String couponCode;
    private String paymentMethod;
    private String notes;
    
    // Getters and Setters
    public Long getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Long shippingAddressId) { this.shippingAddressId = shippingAddressId; }
    public Map<String, Object> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }
    public Map<String, Object> getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Map<String, Object> billingAddress) { this.billingAddress = billingAddress; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
