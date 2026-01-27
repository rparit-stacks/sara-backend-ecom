package com.sara.ecom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.SwipeDto;
import com.sara.ecom.entity.BusinessConfig;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.OrderItem;
import com.sara.ecom.entity.Product;
import com.sara.ecom.entity.User;
import com.sara.ecom.entity.UserAddress;
import com.sara.ecom.repository.ProductRepository;
import com.sara.ecom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SwipeService {
    
    private static final Logger logger = LoggerFactory.getLogger(SwipeService.class);
    
    @Value("${swipe.api.url:https://app.getswipe.in/api/partner/v2}")
    private String swipeApiUrl;
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Create or update customer in Swipe (using party API v2)
     * Note: This method may need to be updated based on Swipe's party/customer API endpoints
     */
    public String createOrUpdateCustomer(User user, UserAddress address) {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            String apiKey = config.getSwipeApiKey();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Swipe API key not configured");
                return null;
            }
            
            SwipeDto.SwipePartyRequest partyRequest = new SwipeDto.SwipePartyRequest();
            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + 
                               " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
            partyRequest.setId("CUST_" + user.getEmail().replace("@", "_").replace(".", "_"));
            partyRequest.setType("customer");
            partyRequest.setName(fullName);
            partyRequest.setEmail(user.getEmail());
            
            // Parse phone number and country code
            String phoneNumber = null;
            String countryCode = "91"; // Default to India
            if (user.getPhoneNumber() != null) {
                String phone = user.getPhoneNumber().trim();
                if (phone.startsWith("+91")) {
                    countryCode = "91";
                    phoneNumber = phone.substring(3).trim();
                } else if (phone.startsWith("91") && phone.length() > 10) {
                    countryCode = "91";
                    phoneNumber = phone.substring(2).trim();
                } else {
                    phoneNumber = phone;
                }
            }
            partyRequest.setPhoneNumber(phoneNumber);
            partyRequest.setCountryCode(countryCode);
            
            if (address != null) {
                SwipeDto.SwipeAddressRequest billingAddress = new SwipeDto.SwipeAddressRequest();
                billingAddress.setAddressLine1(address.getAddress());
                billingAddress.setCity(address.getCity());
                billingAddress.setState(address.getState());
                billingAddress.setPincode(address.getZipCode());
                billingAddress.setCountry("India");
                partyRequest.setBillingAddress(billingAddress);
                partyRequest.setShippingAddress(billingAddress);
                
                // Set GSTIN if available
                if (address.getGstin() != null && !address.getGstin().trim().isEmpty()) {
                    partyRequest.setGstin(address.getGstin());
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<SwipeDto.SwipePartyRequest> request = new HttpEntity<>(partyRequest, headers);
            
            // Note: Swipe API v2 may use different endpoint for party/customer management
            // This endpoint may need to be adjusted based on Swipe's actual API documentation
            String customerUrl = swipeApiUrl + "/customers";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                customerUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object dataObj = body.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    if (data != null && data.get("hashId") != null) {
                        return (String) data.get("hashId");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error creating/updating customer in Swipe", e);
            return null;
        }
    }
    
    private static SwipeDto.SwipeInvoiceResultDto successResult(SwipeDto.SwipeInvoiceData data) {
        SwipeDto.SwipeInvoiceResultDto r = new SwipeDto.SwipeInvoiceResultDto();
        r.setSuccess(true);
        r.setData(data);
        return r;
    }

    private static SwipeDto.SwipeInvoiceResultDto errorResult(String errorSource, String message, String hint) {
        SwipeDto.SwipeInvoiceResultDto r = new SwipeDto.SwipeInvoiceResultDto();
        r.setSuccess(false);
        r.setErrorSource(errorSource);
        r.setMessage(message);
        r.setHint(hint);
        return r;
    }

    /**
     * Create invoice in Swipe from order. Returns result DTO with either success+data or error fields for admin display.
     */
    public SwipeDto.SwipeInvoiceResultDto createInvoice(Order order) {
        // Declare variables outside try block for duplicate error retry
        SwipeDto.SwipeInvoiceRequest invoiceRequest = null;
        SwipeDto.SwipePartyRequest partyRequest = null;
        HttpHeaders headers = null;
        String invoiceUrl = null;
        String apiKey = null;
        
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            
            if (!Boolean.TRUE.equals(config.getSwipeEnabled())) {
                logger.info("Swipe integration is disabled");
                return errorResult("our_system", "Swipe integration is disabled", null);
            }
            
            apiKey = config.getSwipeApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Swipe API key not configured");
                return errorResult("our_system", "Swipe API key not configured", null);
            }
            
            // Check if invoice already exists - prevent duplicate creation
            // Use invoiceStatus as single source of truth (more reliable than just checking swipeInvoiceId)
            if (order.getInvoiceStatus() != null && 
                order.getInvoiceStatus() == com.sara.ecom.entity.Order.InvoiceStatus.CREATED) {
                logger.info("Invoice already CREATED for order {} (status: CREATED), skipping duplicate creation", 
                    order.getId());
                SwipeDto.SwipeInvoiceData data = new SwipeDto.SwipeInvoiceData();
                data.setHashId(order.getSwipeInvoiceId());
                data.setSerialNumber(order.getSwipeInvoiceNumber());
                data.setIrn(order.getSwipeIrn());
                data.setQrCode(order.getSwipeQrCode());
                data.setPdfUrl(order.getSwipeInvoiceUrl());
                return successResult(data);
            }
            
            // Fallback check: if swipeInvoiceId exists but status is not set, also skip
            if (order.getSwipeInvoiceId() != null && !order.getSwipeInvoiceId().trim().isEmpty()) {
                logger.info("Invoice already exists for order {} with hash_id: {} (status not set), skipping creation", 
                    order.getId(), order.getSwipeInvoiceId());
                SwipeDto.SwipeInvoiceData data = new SwipeDto.SwipeInvoiceData();
                data.setHashId(order.getSwipeInvoiceId());
                data.setSerialNumber(order.getSwipeInvoiceNumber());
                data.setIrn(order.getSwipeIrn());
                data.setQrCode(order.getSwipeQrCode());
                data.setPdfUrl(order.getSwipeInvoiceUrl());
                return successResult(data);
            }
            
            // Get user
            User user = userRepository.findByEmail(order.getUserEmail())
                    .orElse(null);
            
            // Parse shipping address from JSON
            Map<String, Object> shippingAddressMap = new HashMap<>();
            if (order.getShippingAddress() != null) {
                try {
                    Map<String, Object> parsed = objectMapper.readValue(
                        order.getShippingAddress(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                    shippingAddressMap = parsed;
                } catch (Exception e) {
                    logger.warn("Error parsing shipping address", e);
                }
            }
            
            // Create party request (replaces customer in API v2)
            partyRequest = new SwipeDto.SwipePartyRequest();
            partyRequest.setId("CUST_" + order.getUserEmail().replace("@", "_").replace(".", "_"));
            partyRequest.setType("customer");
            partyRequest.setName(order.getUserName());
            partyRequest.setEmail(order.getUserEmail());
            
            // Parse phone number and country code - use shipping address phone (single source of truth)
            String phoneNumber = null;
            String countryCode = "91"; // Default to India
            
            // First try shipping address phone (single source of truth)
            if (shippingAddressMap.containsKey("phone")) {
                phoneNumber = (String) shippingAddressMap.get("phone");
            } else if (shippingAddressMap.containsKey("phoneNumber")) {
                phoneNumber = (String) shippingAddressMap.get("phoneNumber");
            }
            
            // Fallback to user profile phone if not in shipping address
            if ((phoneNumber == null || phoneNumber.trim().isEmpty()) && user != null && user.getPhoneNumber() != null) {
                phoneNumber = user.getPhoneNumber().trim();
            }
            
            // Format phone number (remove spaces, dashes, etc.)
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String phone = phoneNumber.trim();
                // If phone starts with +91 or 91, extract country code
                if (phone.startsWith("+91")) {
                    countryCode = "91";
                    phoneNumber = phone.substring(3).trim();
                } else if (phone.startsWith("91") && phone.length() > 10) {
                    countryCode = "91";
                    phoneNumber = phone.substring(2).trim();
                } else {
                    // Remove any non-digit characters for clean phone number
                    phoneNumber = phone.replaceAll("[^0-9]", "");
                }
            }
            
            partyRequest.setPhoneNumber(phoneNumber);
            partyRequest.setCountryCode(countryCode);
            
            // Set billing address from shipping address
            SwipeDto.SwipeAddressRequest billingAddress = new SwipeDto.SwipeAddressRequest();
            // address_line1 is required - use address field or default
            String addressLine1 = (String) shippingAddressMap.getOrDefault("address", "");
            if (addressLine1 == null || addressLine1.trim().isEmpty()) {
                addressLine1 = "Address not provided"; // Swipe requires non-empty address_line1
                logger.warn("Empty address_line1 for order {}, using default text", order.getId());
            }
            billingAddress.setAddressLine1(addressLine1.trim());
            // address_line2 is required by Swipe API - always set (empty string if not provided)
            String addressLine2 = "";
            if (shippingAddressMap.containsKey("addressLine2")) {
                addressLine2 = (String) shippingAddressMap.get("addressLine2");
            } else if (shippingAddressMap.containsKey("address_line2")) {
                addressLine2 = (String) shippingAddressMap.get("address_line2");
            }
            billingAddress.setAddressLine2(addressLine2 != null ? addressLine2.trim() : "");
            if (shippingAddressMap.containsKey("city")) {
                billingAddress.setCity((String) shippingAddressMap.get("city"));
            }
            if (shippingAddressMap.containsKey("state")) {
                String state = (String) shippingAddressMap.get("state");
                // Convert state name to Swipe's expected format (uppercase)
                billingAddress.setState(normalizeStateName(state));
            }
            if (shippingAddressMap.containsKey("postalCode") || shippingAddressMap.containsKey("zipCode")) {
                String pincode = (String) shippingAddressMap.getOrDefault("postalCode", shippingAddressMap.get("zipCode"));
                billingAddress.setPincode(pincode);
            }
            String rawCountry = shippingAddressMap.containsKey("country")
                ? (String) shippingAddressMap.get("country")
                : "India";
            billingAddress.setCountry(normalizeCountryName(rawCountry));
            // addr_id is deprecated in Swipe API v2 - use addr_id_v2 instead.
            // addr_id_v2 must be alphanumeric, non-empty, and <= 16 chars.
            Long baseAddrId = order.getShippingAddressId() != null ? order.getShippingAddressId() : order.getId();
            long safe = baseAddrId != null ? Math.abs(baseAddrId) : 0L;
            // keep up to 12 digits so "ADDR" + digits fits in 16 chars
            long shortId = safe % 1_000_000_000_000L; // 0..999999999999
            String addrIdV2 = "ADDR" + shortId; // alphanumeric only, <= 16 chars
            billingAddress.setAddrId(null); // do not send addr_id
            billingAddress.setAddrIdV2(addrIdV2);
            partyRequest.setBillingAddress(billingAddress);
            partyRequest.setShippingAddress(billingAddress); // Use same address for shipping
            
            // Set GSTIN if available (only set if not null/empty, otherwise leave null - won't be serialized)
            if (shippingAddressMap.containsKey("gstin")) {
                String gstin = (String) shippingAddressMap.get("gstin");
                if (gstin != null && !gstin.trim().isEmpty()) {
                    partyRequest.setGstin(gstin.trim());
                }
            }
            
            // Create invoice items
            List<SwipeDto.SwipeInvoiceItemRequest> items = new ArrayList<>();
            for (OrderItem orderItem : order.getItems()) {
                SwipeDto.SwipeInvoiceItemRequest item = new SwipeDto.SwipeInvoiceItemRequest();
                
                // Generate unique item ID
                String itemId = "ITEM_" + (orderItem.getProductId() != null ? orderItem.getProductId() : orderItem.getId());
                item.setId(itemId);
                item.setName(orderItem.getName());
                item.setQuantity(orderItem.getQuantity());
                item.setUnitPrice(orderItem.getPrice());
                
                // Get product for GST rate and HSN code
                Product product = null;
                if (orderItem.getProductId() != null) {
                    product = productRepository.findById(orderItem.getProductId()).orElse(null);
                }
                
                BigDecimal gstRate = BigDecimal.ZERO;
                if (product != null && product.getGstRate() != null) {
                    gstRate = product.getGstRate();
                }
                item.setTaxRate(gstRate);
                
                // IMPORTANT: In our system, OrderItem.price/totalPrice are NET (GST calculated separately in CartService).
                // Swipe expects consistent amounts:
                // - unit_price: net per unit
                // - net_amount: net line total (= unit_price * quantity)
                // - price_with_tax: gross per unit (= unit_price * (1 + tax_rate/100))
                // - total_amount: gross line total (= price_with_tax * quantity)
                BigDecimal unitPriceNet = orderItem.getPrice() != null ? orderItem.getPrice() : BigDecimal.ZERO;
                Integer qty = orderItem.getQuantity() != null ? orderItem.getQuantity() : 1;
                BigDecimal netLineTotal = orderItem.getTotalPrice();
                if (netLineTotal == null) {
                    netLineTotal = unitPriceNet.multiply(BigDecimal.valueOf(qty));
                }
                
                BigDecimal grossUnit = unitPriceNet;
                if (gstRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiplier = BigDecimal.ONE.add(gstRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                    grossUnit = unitPriceNet.multiply(multiplier);
                }
                
                // Round to 2 decimals for money fields
                grossUnit = grossUnit.setScale(2, RoundingMode.HALF_UP);
                netLineTotal = netLineTotal.setScale(2, RoundingMode.HALF_UP);
                BigDecimal grossLineTotal = grossUnit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
                
                item.setUnitPrice(unitPriceNet.setScale(2, RoundingMode.HALF_UP));
                item.setNetAmount(netLineTotal);
                item.setPriceWithTax(grossUnit);
                item.setTotalAmount(grossLineTotal);
                
                // Set HSN code - validate and format
                String hsnCode = null;
                if (product != null && product.getHsnCode() != null) {
                    // Clean HSN code - remove spaces, dashes, and other non-numeric characters
                    hsnCode = product.getHsnCode().trim().replaceAll("[^0-9]", "");
                }
                
                // Validate HSN code (should be numeric, 4-8 digits as per GST rules)
                if (hsnCode == null || hsnCode.isEmpty() || !hsnCode.matches("^[0-9]{4,8}$")) {
                    // Use default HSN code for garments/clothing (Chapter 61 - Articles of apparel)
                    hsnCode = "6109";
                    if (product != null && product.getHsnCode() != null) {
                        logger.warn("Invalid HSN code '{}' for product {} (order item {}), using default: {}", 
                            product.getHsnCode(), orderItem.getProductId(), orderItem.getId(), hsnCode);
                    } else {
                        logger.debug("Missing HSN code for product {} (order item {}), using default: {}", 
                            orderItem.getProductId(), orderItem.getId(), hsnCode);
                    }
                }
                item.setHsnCode(hsnCode);
                
                item.setItemType("Product");
                items.add(item);
            }
            
            // Create invoice request
            invoiceRequest = new SwipeDto.SwipeInvoiceRequest();
            invoiceRequest.setDocumentType("invoice");
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String documentDate = order.getCreatedAt().format(dateFormatter);
            invoiceRequest.setDocumentDate(documentDate);
            
            // Set due date (same as document date or 30 days later)
            invoiceRequest.setDueDate(documentDate);
            
            // serial_number is deprecated in Swipe API v2 - use serial_number_v2
            invoiceRequest.setSerialNumber(null);
            SwipeDto.SwipeSerialNumberV2 serialV2 = new SwipeDto.SwipeSerialNumberV2();
            serialV2.setPrefix(null);
            Integer docNumber = null;
            try {
                docNumber = Integer.valueOf(order.getOrderNumber());
            } catch (Exception ignore) {
                if (order.getId() != null) {
                    docNumber = (int) (order.getId() % Integer.MAX_VALUE);
                }
            }
            serialV2.setDocNumber(docNumber != null ? docNumber : 0);
            serialV2.setSuffix(null);
            invoiceRequest.setSerialNumberV2(serialV2);
            invoiceRequest.setParty(partyRequest);
            invoiceRequest.setItems(items);
            invoiceRequest.setExtraDiscount(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO);
            // Force-disable e-invoice unless it's enabled in Swipe portal for this company.
            // Swipe returns: "E-Invoice is not enabled for this company." when this is true.
            invoiceRequest.setEinvoice(false);
            
            // Add notes if available (only set if not null/empty, otherwise leave null - won't be serialized)
            if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
                invoiceRequest.setNotes(order.getNotes().trim());
            }
            // Terms can be left null - won't be serialized due to @JsonInclude(NON_NULL)
            
            // Call Swipe API v2
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // Log request for debugging (without sensitive data)
            try {
                String requestJson = objectMapper.writeValueAsString(invoiceRequest);
                logger.debug("Swipe API request payload: {}", requestJson);
            } catch (Exception e) {
                logger.warn("Failed to serialize request for logging", e);
            }
            
            HttpEntity<SwipeDto.SwipeInvoiceRequest> request = new HttpEntity<>(invoiceRequest, headers);
            
            invoiceUrl = swipeApiUrl + "/doc";
            logger.info("Creating invoice in Swipe for order: {} with document_date: {}", 
                order.getId(), invoiceRequest.getDocumentDate());
            
            ResponseEntity<SwipeDto.SwipeInvoiceResponse> response = restTemplate.exchange(
                invoiceUrl,
                HttpMethod.POST,
                request,
                SwipeDto.SwipeInvoiceResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SwipeDto.SwipeInvoiceResponse invoiceResponse = response.getBody();
                if (invoiceResponse.getSuccess() != null && invoiceResponse.getSuccess()) {
                    logger.info("Successfully created invoice in Swipe for order: {}", order.getId());
                    return successResult(invoiceResponse.getData());
                } else {
                    String msg = invoiceResponse.getMessage() != null ? invoiceResponse.getMessage() : "Swipe returned unsuccessful";
                    logger.warn("Swipe API returned unsuccessful response for order {}: {}", order.getId(), msg);
                    return errorResult("swipe", msg, null);
                }
            }
            
            logger.warn("Failed to create invoice in Swipe for order: {}. Status: {}", 
                order.getId(), response.getStatusCode());
            return errorResult("swipe", "Swipe API returned " + response.getStatusCode(), null);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("Network error creating invoice in Swipe for order: " + order.getId() + 
                ". This may be due to network connectivity issues or incorrect API URL.", e);
            return errorResult("swipe", "Network error: " + (e.getMessage() != null ? e.getMessage() : "Unable to reach Swipe"), null);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.error("HTTP error creating invoice in Swipe for order: " + order.getId() + 
                ". Status: " + e.getStatusCode() + ", Response: " + responseBody, e);
            
            // Handle EXISTING_CUSTOMER error - customer already exists in Swipe
            // Retry once without party.id to let Swipe map the existing customer by email/phone.
            if (e.getStatusCode().value() == 400 && responseBody != null &&
                responseBody.contains("EXISTING_CUSTOMER") && invoiceRequest != null &&
                invoiceUrl != null && headers != null) {
                String customerId = partyRequest != null ? partyRequest.getId() : "unknown";
                logger.warn("Customer already exists in Swipe for order {} (ID: {}). Retrying invoice without party.id.",
                    order.getId(), customerId);
                try {
                    if (partyRequest != null) {
                        partyRequest.setId(null);
                    }
                    ResponseEntity<SwipeDto.SwipeInvoiceResponse> retryResponse = restTemplate.exchange(
                        invoiceUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(invoiceRequest, headers),
                        SwipeDto.SwipeInvoiceResponse.class
                    );
                    if (retryResponse.getStatusCode().is2xxSuccessful() && retryResponse.getBody() != null) {
                        SwipeDto.SwipeInvoiceResponse retryInvoiceResponse = retryResponse.getBody();
                        if (retryInvoiceResponse.getSuccess() != null && retryInvoiceResponse.getSuccess()) {
                            logger.info("Successfully created invoice in Swipe for order {} after EXISTING_CUSTOMER retry", order.getId());
                            return successResult(retryInvoiceResponse.getData());
                        }
                    }
                } catch (Exception retryException) {
                    logger.warn("Failed to retry invoice creation without party.id for order {}", order.getId(), retryException);
                }
            }
            
            // Handle DUPLICATE_DOC_SERIAL_NUMBER - extract suggested serial number and retry once
            if (e.getStatusCode().value() == 400 && responseBody != null && 
                responseBody.contains("DUPLICATE_DOC_SERIAL_NUMBER") && invoiceRequest != null && 
                invoiceUrl != null && headers != null && apiKey != null) {
                try {
                    // Parse response to extract suggested serial number
                    Map<String, Object> errorResponse = objectMapper.readValue(
                        responseBody, 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                    String message = (String) errorResponse.get("message");
                    if (message != null && message.contains("will be")) {
                        // Extract number from message like "The new serial number will be 8082614."
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("will be (\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(message);
                        if (matcher.find()) {
                            String suggestedSerial = matcher.group(1);
                            logger.info("Duplicate serial number detected for order {}, retrying with suggested serial: {}", 
                                order.getId(), suggestedSerial);
                            
                            // Update serial_number_v2 with suggested number and retry once
                            SwipeDto.SwipeSerialNumberV2 retrySerialV2 = new SwipeDto.SwipeSerialNumberV2();
                            retrySerialV2.setPrefix(null);
                            try {
                                retrySerialV2.setDocNumber(Integer.valueOf(suggestedSerial));
                            } catch (Exception ignore) {
                                retrySerialV2.setDocNumber(0);
                            }
                            retrySerialV2.setSuffix(null);
                            invoiceRequest.setSerialNumberV2(retrySerialV2);
                            
                            // Retry once with new serial number
                            ResponseEntity<SwipeDto.SwipeInvoiceResponse> retryResponse = restTemplate.exchange(
                                invoiceUrl,
                                HttpMethod.POST,
                                new HttpEntity<>(invoiceRequest, headers),
                                SwipeDto.SwipeInvoiceResponse.class
                            );
                            
                            if (retryResponse.getStatusCode().is2xxSuccessful() && retryResponse.getBody() != null) {
                                SwipeDto.SwipeInvoiceResponse retryInvoiceResponse = retryResponse.getBody();
                                if (retryInvoiceResponse.getSuccess() != null && retryInvoiceResponse.getSuccess()) {
                                    logger.info("Successfully created invoice in Swipe for order {} after retry with suggested serial", order.getId());
                                    return successResult(retryInvoiceResponse.getData());
                                }
                            }
                        }
                    }
                } catch (Exception retryException) {
                    logger.warn("Failed to retry with suggested serial number for order {}", order.getId(), retryException);
                }
            }

            String errMsg = responseBody != null && !responseBody.isEmpty() ? responseBody : ("Swipe API error: " + e.getStatusCode());
            String src = "swipe";
            String hint = null;
            if (responseBody != null) {
                if (responseBody.contains("EXISTING_CUSTOMER") || responseBody.contains("DUPLICATE_DOC_SERIAL_NUMBER")) {
                    src = "our_system";
                }
                if (responseBody.contains("HSN") || responseBody.contains("hsn")) {
                    src = "our_system";
                    hint = "Check and correct HSN codes for all products (4–8 digits).";
                }
            }
            return errorResult(src, errMsg.length() > 500 ? errMsg.substring(0, 500) + "…" : errMsg, hint);
        } catch (Exception e) {
            logger.error("Error creating invoice in Swipe for order: " + order.getId(), e);
            return errorResult("swipe", e.getMessage() != null ? e.getMessage() : "Unexpected error creating invoice", null);
        }
    }
    
    /**
     * Get invoice details from Swipe
     */
    public SwipeDto.SwipeInvoiceResponse getInvoiceDetails(String hashId) {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            String apiKey = config.getSwipeApiKey();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Swipe API key not configured");
                return null;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // Use /doc/{hash_id} endpoint for API v2
            String invoiceUrl = swipeApiUrl + "/doc/" + hashId;
            logger.info("Fetching invoice details from Swipe for hash_id: {}", hashId);
            
            ResponseEntity<SwipeDto.SwipeInvoiceResponse> response = restTemplate.exchange(
                invoiceUrl,
                HttpMethod.GET,
                request,
                SwipeDto.SwipeInvoiceResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SwipeDto.SwipeInvoiceResponse invoiceResponse = response.getBody();
                if (invoiceResponse.getSuccess() != null && invoiceResponse.getSuccess()) {
                    logger.info("Successfully fetched invoice details from Swipe for hash_id: {}", hashId);
                    return invoiceResponse;
                } else {
                    logger.warn("Swipe API returned unsuccessful response for hash_id {}: {}", 
                        hashId, invoiceResponse.getMessage());
                    return null;
                }
            }
            
            logger.warn("Failed to fetch invoice details from Swipe for hash_id: {}. Status: {}", 
                hashId, response.getStatusCode());
            return null;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("Network error fetching invoice details from Swipe for hash_id: " + hashId + 
                ". This may be due to network connectivity issues or incorrect API URL.", e);
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP error fetching invoice details from Swipe for hash_id: " + hashId + 
                ". Status: " + e.getStatusCode() + ", Response: " + e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching invoice details from Swipe for hash_id: " + hashId, e);
            return null;
        }
    }
    
    /**
     * Normalize state name to Swipe API's expected format (uppercase)
     * Maps common state name variations to Swipe's expected values
     */
    private String normalizeStateName(String state) {
        if (state == null || state.trim().isEmpty()) {
            return "OTHER TERRITORY";
        }
        
        String normalized = state.trim().toUpperCase();
        
        // Map common variations to Swipe's expected format
        Map<String, String> stateMapping = new HashMap<>();
        stateMapping.put("DELHI", "DELHI");
        stateMapping.put("NEW DELHI", "DELHI");
        stateMapping.put("ANDHRA PRADESH", "ANDHRA PRADESH");
        stateMapping.put("ANDHRAPRADESH", "ANDHRA PRADESH");
        stateMapping.put("TAMILNADU", "TAMIL NADU");
        stateMapping.put("TAMIL NADU", "TAMIL NADU");
        stateMapping.put("WEST BENGAL", "WEST BENGAL");
        stateMapping.put("WB", "WEST BENGAL");
        stateMapping.put("UTTAR PRADESH", "UTTAR PRADESH");
        stateMapping.put("UP", "UTTAR PRADESH");
        stateMapping.put("MADHYA PRADESH", "MADHYA PRADESH");
        stateMapping.put("MP", "MADHYA PRADESH");
        stateMapping.put("RAJASTHAN", "RAJASTHAN");
        stateMapping.put("RJ", "RAJASTHAN");
        stateMapping.put("GUJARAT", "GUJARAT");
        stateMapping.put("GJ", "GUJARAT");
        stateMapping.put("KARNATAKA", "KARNATAKA");
        stateMapping.put("KA", "KARNATAKA");
        stateMapping.put("MAHARASHTRA", "MAHARASHTRA");
        stateMapping.put("MH", "MAHARASHTRA");
        stateMapping.put("PUNJAB", "PUNJAB");
        stateMapping.put("PB", "PUNJAB");
        stateMapping.put("HARYANA", "HARYANA");
        stateMapping.put("HR", "HARYANA");
        stateMapping.put("KERALA", "KERALA");
        stateMapping.put("KL", "KERALA");
        stateMapping.put("ODISHA", "ODISHA");
        stateMapping.put("OR", "ODISHA");
        stateMapping.put("JHARKHAND", "JHARKHAND");
        stateMapping.put("JH", "JHARKHAND");
        stateMapping.put("ASSAM", "ASSAM");
        stateMapping.put("AS", "ASSAM");
        stateMapping.put("BIHAR", "BIHAR");
        stateMapping.put("BR", "BIHAR");
        stateMapping.put("CHHATTISGARH", "CHHATTISGARH");
        stateMapping.put("CG", "CHHATTISGARH");
        stateMapping.put("GOA", "GOA");
        stateMapping.put("GA", "GOA");
        stateMapping.put("HIMACHAL PRADESH", "HIMACHAL PRADESH");
        stateMapping.put("HP", "HIMACHAL PRADESH");
        stateMapping.put("JAMMU AND KASHMIR", "JAMMU AND KASHMIR");
        stateMapping.put("JK", "JAMMU AND KASHMIR");
        stateMapping.put("MANIPUR", "MANIPUR");
        stateMapping.put("MN", "MANIPUR");
        stateMapping.put("MEGHALAYA", "MEGHALAYA");
        stateMapping.put("ML", "MEGHALAYA");
        stateMapping.put("MIZORAM", "MIZORAM");
        stateMapping.put("MZ", "MIZORAM");
        stateMapping.put("NAGALAND", "NAGALAND");
        stateMapping.put("NL", "NAGALAND");
        stateMapping.put("SIKKIM", "SIKKIM");
        stateMapping.put("SK", "SIKKIM");
        stateMapping.put("TRIPURA", "TRIPURA");
        stateMapping.put("TR", "TRIPURA");
        stateMapping.put("ARUNACHAL PRADESH", "ARUNACHAL PRADESH");
        stateMapping.put("AR", "ARUNACHAL PRADESH");
        stateMapping.put("TELANGANA", "TELANGANA");
        stateMapping.put("TS", "TELANGANA");
        stateMapping.put("UTTARAKHAND", "UTTARAKHAND");
        stateMapping.put("UK", "UTTARAKHAND");
        stateMapping.put("CHANDIGARH", "CHANDIGARH");
        stateMapping.put("CH", "CHANDIGARH");
        stateMapping.put("DADRA & NAGAR HAVELI & DAMAN & DIU", "DADRA & NAGAR HAVELI & DAMAN & DIU");
        stateMapping.put("DADRA AND NAGAR HAVELI AND DAMAN AND DIU", "DADRA & NAGAR HAVELI & DAMAN & DIU");
        stateMapping.put("LAKSHWADEEP", "LAKSHWADEEP");
        stateMapping.put("LD", "LAKSHWADEEP");
        stateMapping.put("PUDUCHERRY", "PUDUCHERRY");
        stateMapping.put("PY", "PUDUCHERRY");
        stateMapping.put("ANDAMAN & NICOBAR", "ANDAMAN & NICOBAR");
        stateMapping.put("ANDAMAN AND NICOBAR", "ANDAMAN & NICOBAR");
        stateMapping.put("AN", "ANDAMAN & NICOBAR");
        stateMapping.put("LADAKH", "LADAKH(NEWLYADDED)");
        stateMapping.put("LADAKH(NEWLYADDED)", "LADAKH(NEWLYADDED)");
        
        // Check if exact match exists
        if (stateMapping.containsKey(normalized)) {
            return stateMapping.get(normalized);
        }
        
        // Check if it matches any of Swipe's expected values (case-insensitive)
        String[] swipeStates = {
            "JAMMU AND KASHMIR", "HIMACHAL PRADESH", "PUNJAB", "CHANDIGARH", "UTTARAKHAND",
            "HARYANA", "DELHI", "RAJASTHAN", "UTTAR PRADESH", "BIHAR", "SIKKIM",
            "ARUNACHAL PRADESH", "NAGALAND", "MANIPUR", "MIZORAM", "TRIPURA", "MEGHALAYA",
            "ASSAM", "WEST BENGAL", "JHARKHAND", "ODISHA", "CHHATTISGARH", "MADHYA PRADESH",
            "GUJARAT", "DADRA & NAGAR HAVELI & DAMAN & DIU", "MAHARASHTRA",
            "ANDHRAPRADESH(BEFOREADDED)", "KARNATAKA", "GOA", "LAKSHWADEEP", "KERALA",
            "TAMIL NADU", "PUDUCHERRY", "ANDAMAN & NICOBAR", "TELANGANA", "ANDHRA PRADESH",
            "LADAKH(NEWLYADDED)", "OTHER TERRITORY"
        };
        
        for (String swipeState : swipeStates) {
            if (swipeState.equalsIgnoreCase(normalized)) {
                return swipeState;
            }
        }
        
        // If no match found, return as uppercase (might work) or default to OTHER TERRITORY
        logger.warn("State name '{}' not found in Swipe's expected list, using as-is: {}", state, normalized);
        return normalized;
    }

    /**
     * Normalize country name for Swipe API.
     * Swipe expects full country names (e.g. "India"), not ISO codes (e.g. "IN").
     */
    private String normalizeCountryName(String country) {
        if (country == null || country.trim().isEmpty()) {
            return "India";
        }

        String trimmed = country.trim();
        String upper = trimmed.toUpperCase();

        // Common ISO2 -> Name mapping (extend if needed)
        if ("IN".equals(upper) || "IND".equals(upper)) return "India";
        if ("US".equals(upper) || "USA".equals(upper)) return "United States";
        if ("GB".equals(upper) || "UK".equals(upper)) return "United Kingdom";
        if ("AE".equals(upper) || "UAE".equals(upper)) return "United Arab Emirates";

        // Already a full name (e.g. "India") - pass through
        return trimmed;
    }
}
