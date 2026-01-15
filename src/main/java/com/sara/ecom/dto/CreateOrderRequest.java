package com.sara.ecom.dto;

import java.util.List;
import java.util.Map;

public class CreateOrderRequest {
    private Long shippingAddressId;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private String couponCode;
    private String paymentMethod;
    private String notes;
    
    // Guest checkout fields
    private String guestEmail;
    private String guestFirstName;
    private String guestLastName;
    private String guestPhone;
    private List<AddToCartRequest> guestCartItems; // Cart items for guest checkout
    
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
    
    // Guest checkout getters and setters
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public String getGuestFirstName() { return guestFirstName; }
    public void setGuestFirstName(String guestFirstName) { this.guestFirstName = guestFirstName; }
    public String getGuestLastName() { return guestLastName; }
    public void setGuestLastName(String guestLastName) { this.guestLastName = guestLastName; }
    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
    public List<AddToCartRequest> getGuestCartItems() { return guestCartItems; }
    public void setGuestCartItems(List<AddToCartRequest> guestCartItems) { this.guestCartItems = guestCartItems; }
}
