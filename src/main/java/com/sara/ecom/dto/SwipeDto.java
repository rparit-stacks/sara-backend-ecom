package com.sara.ecom.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SwipeDto {
    
    // Customer Request
    public static class SwipeCustomerRequest {
        private String name;
        private String email;
        private String phone;
        private SwipeAddressRequest billingAddress;
        private String gstin;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public SwipeAddressRequest getBillingAddress() { return billingAddress; }
        public void setBillingAddress(SwipeAddressRequest billingAddress) { this.billingAddress = billingAddress; }
        public String getGstin() { return gstin; }
        public void setGstin(String gstin) { this.gstin = gstin; }
    }
    
    public static class SwipeAddressRequest {
        private String address;
        private String city;
        private String state;
        private String pincode;
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
    }
    
    // Invoice Item Request
    public static class SwipeInvoiceItemRequest {
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal netAmount;
        private BigDecimal totalAmount;
        private BigDecimal taxRate;
        private String hsnCode;
        private String itemType;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getNetAmount() { return netAmount; }
        public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getTaxRate() { return taxRate; }
        public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
        public String getHsnCode() { return hsnCode; }
        public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
    }
    
    // Invoice Request
    public static class SwipeInvoiceRequest {
        private String documentType = "invoice";
        private String documentDate;
        private String serialNumber;
        private SwipeCustomerRequest customer;
        private List<SwipeInvoiceItemRequest> items;
        private BigDecimal subtotal;
        private BigDecimal extraDiscount;
        private BigDecimal shipping;
        private BigDecimal total;
        private Boolean einvoice;
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDocumentDate() { return documentDate; }
        public void setDocumentDate(String documentDate) { this.documentDate = documentDate; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public SwipeCustomerRequest getCustomer() { return customer; }
        public void setCustomer(SwipeCustomerRequest customer) { this.customer = customer; }
        public List<SwipeInvoiceItemRequest> getItems() { return items; }
        public void setItems(List<SwipeInvoiceItemRequest> items) { this.items = items; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getExtraDiscount() { return extraDiscount; }
        public void setExtraDiscount(BigDecimal extraDiscount) { this.extraDiscount = extraDiscount; }
        public BigDecimal getShipping() { return shipping; }
        public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public Boolean getEinvoice() { return einvoice; }
        public void setEinvoice(Boolean einvoice) { this.einvoice = einvoice; }
    }
    
    // Invoice Response
    public static class SwipeInvoiceResponse {
        private Boolean success;
        private SwipeInvoiceData data;
        private String message;
        private Map<String, Object> error;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public SwipeInvoiceData getData() { return data; }
        public void setData(SwipeInvoiceData data) { this.data = data; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getError() { return error; }
        public void setError(Map<String, Object> error) { this.error = error; }
    }
    
    public static class SwipeInvoiceData {
        private String hashId;
        private String serialNumber;
        private String irn;
        private String qrCode;
        private String pdfUrl;
        
        public String getHashId() { return hashId; }
        public void setHashId(String hashId) { this.hashId = hashId; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public String getIrn() { return irn; }
        public void setIrn(String irn) { this.irn = irn; }
        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public String getPdfUrl() { return pdfUrl; }
        public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    }
    
    // Error Response
    public static class SwipeErrorResponse {
        private Boolean success;
        private String message;
        private Map<String, Object> error;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getError() { return error; }
        public void setError(Map<String, Object> error) { this.error = error; }
    }
}
