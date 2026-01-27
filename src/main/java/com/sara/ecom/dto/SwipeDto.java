package com.sara.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SwipeDto {
    
    // Party Request (replaces Customer in API v2)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipePartyRequest {
        private String id;
        private String type = "customer";
        private String name;
        private String email;
        
        @JsonProperty("phone_number")
        private String phoneNumber;
        
        @JsonProperty("country_code")
        private String countryCode;
        
        @JsonProperty("billing_address")
        private SwipeAddressRequest billingAddress;
        
        @JsonProperty("shipping_address")
        private SwipeAddressRequest shippingAddress;
        
        private String gstin;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public SwipeAddressRequest getBillingAddress() { return billingAddress; }
        public void setBillingAddress(SwipeAddressRequest billingAddress) { this.billingAddress = billingAddress; }
        public SwipeAddressRequest getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(SwipeAddressRequest shippingAddress) { this.shippingAddress = shippingAddress; }
        public String getGstin() { return gstin; }
        public void setGstin(String gstin) { this.gstin = gstin; }
    }
    
    // Address Request (matches Swipe API v2 format)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipeAddressRequest {
        @JsonProperty("address_line1")
        private String addressLine1;
        
        @JsonProperty("address_line2")
        private String addressLine2;
        
        private String city;
        private String state;
        private String country;
        private String pincode;
        
        @JsonProperty("addr_id")
        private Integer addrId; // Optional: use 0 for new address or omit if using addr_id_v2
        
        @JsonProperty("addr_id_v2")
        private String addrIdV2; // Optional: alphanumeric address ID
        
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
        public Integer getAddrId() { return addrId; }
        public void setAddrId(Integer addrId) { this.addrId = addrId; }
        public String getAddrIdV2() { return addrIdV2; }
        public void setAddrIdV2(String addrIdV2) { this.addrIdV2 = addrIdV2; }
    }
    
    // Invoice Item Request (matches Swipe API v2 format)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipeInvoiceItemRequest {
        private String id;
        private String name;
        private Integer quantity;
        
        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
        
        @JsonProperty("tax_rate")
        private BigDecimal taxRate;
        
        @JsonProperty("price_with_tax")
        private BigDecimal priceWithTax;
        
        @JsonProperty("net_amount")
        private BigDecimal netAmount;
        
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        
        @JsonProperty("item_type")
        private String itemType;
        
        @JsonProperty("hsn_code")
        private String hsnCode;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getTaxRate() { return taxRate; }
        public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
        public BigDecimal getPriceWithTax() { return priceWithTax; }
        public void setPriceWithTax(BigDecimal priceWithTax) { this.priceWithTax = priceWithTax; }
        public BigDecimal getNetAmount() { return netAmount; }
        public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public String getHsnCode() { return hsnCode; }
        public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
    }
    
    // Invoice Request (matches Swipe API v2 format)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipeInvoiceRequest {
        @JsonProperty("document_type")
        private String documentType = "invoice";
        
        @JsonProperty("document_date")
        private String documentDate;
        
        @JsonProperty("due_date")
        private String dueDate;
        
        @JsonProperty("serial_number")
        private String serialNumber;

        @JsonProperty("serial_number_v2")
        private SwipeSerialNumberV2 serialNumberV2;
        
        private SwipePartyRequest party;
        private List<SwipeInvoiceItemRequest> items;
        
        @JsonProperty("extra_discount")
        private BigDecimal extraDiscount;
        
        private String notes;
        private String terms;
        
        @JsonProperty("einvoice")
        private Boolean einvoice;
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDocumentDate() { return documentDate; }
        public void setDocumentDate(String documentDate) { this.documentDate = documentDate; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public SwipeSerialNumberV2 getSerialNumberV2() { return serialNumberV2; }
        public void setSerialNumberV2(SwipeSerialNumberV2 serialNumberV2) { this.serialNumberV2 = serialNumberV2; }
        public SwipePartyRequest getParty() { return party; }
        public void setParty(SwipePartyRequest party) { this.party = party; }
        public List<SwipeInvoiceItemRequest> getItems() { return items; }
        public void setItems(List<SwipeInvoiceItemRequest> items) { this.items = items; }
        public BigDecimal getExtraDiscount() { return extraDiscount; }
        public void setExtraDiscount(BigDecimal extraDiscount) { this.extraDiscount = extraDiscount; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getTerms() { return terms; }
        public void setTerms(String terms) { this.terms = terms; }
        public Boolean getEinvoice() { return einvoice; }
        public void setEinvoice(Boolean einvoice) { this.einvoice = einvoice; }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipeSerialNumberV2 {
        private String prefix;

        @JsonProperty("doc_number")
        private Integer docNumber;

        private String suffix;

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public Integer getDocNumber() { return docNumber; }
        public void setDocNumber(Integer docNumber) { this.docNumber = docNumber; }
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
    }
    
    // Invoice Response (matches Swipe API v2 format)
    public static class SwipeInvoiceResponse {
        private Boolean success;
        private SwipeInvoiceData data;
        private String message;
        private String errorCode;
        // Swipe sometimes returns {} and sometimes [] for "errors", so keep it flexible.
        private Object errors;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public SwipeInvoiceData getData() { return data; }
        public void setData(SwipeInvoiceData data) { this.data = data; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public Object getErrors() { return errors; }
        public void setErrors(Object errors) { this.errors = errors; }
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
        private String errorCode;
        // Swipe sometimes returns {} and sometimes [] for "errors", so keep it flexible.
        private Object errors;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public Object getErrors() { return errors; }
        public void setErrors(Object errors) { this.errors = errors; }
    }

    /**
     * Result of invoice creation: either success + data or failure + error fields for admin display.
     * errorSource is "our_system" (validation, HSN, address, etc.) or "swipe" (Swipe API/network).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SwipeInvoiceResultDto {
        private Boolean success;
        private SwipeInvoiceData data;
        private String errorSource;
        private String errorCode;
        private String message;
        private String hint;

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public SwipeInvoiceData getData() { return data; }
        public void setData(SwipeInvoiceData data) { this.data = data; }
        public String getErrorSource() { return errorSource; }
        public void setErrorSource(String errorSource) { this.errorSource = errorSource; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }
    }
}
