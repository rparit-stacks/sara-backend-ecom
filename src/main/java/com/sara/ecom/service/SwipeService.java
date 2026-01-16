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
    
    @Value("${swipe.api.url:https://api.getswipe.in/v1}")
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
     * Create or update customer in Swipe
     */
    public String createOrUpdateCustomer(User user, UserAddress address) {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            String apiKey = config.getSwipeApiKey();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Swipe API key not configured");
                return null;
            }
            
            SwipeDto.SwipeCustomerRequest customerRequest = new SwipeDto.SwipeCustomerRequest();
            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + 
                               " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
            customerRequest.setName(fullName);
            customerRequest.setEmail(user.getEmail());
            customerRequest.setPhone(user.getPhoneNumber());
            
            if (address != null) {
                SwipeDto.SwipeAddressRequest billingAddress = new SwipeDto.SwipeAddressRequest();
                billingAddress.setAddress(address.getAddress());
                billingAddress.setCity(address.getCity());
                billingAddress.setState(address.getState());
                billingAddress.setPincode(address.getZipCode());
                customerRequest.setBillingAddress(billingAddress);
                
                // Set GSTIN if available
                if (address.getGstin() != null && !address.getGstin().trim().isEmpty()) {
                    customerRequest.setGstin(address.getGstin());
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<SwipeDto.SwipeCustomerRequest> request = new HttpEntity<>(customerRequest, headers);
            
            // Try to create/update customer
            // Note: Swipe API endpoint may vary - adjust as needed
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
    
    /**
     * Create invoice in Swipe from order
     */
    public SwipeDto.SwipeInvoiceResponse createInvoice(Order order) {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            
            if (!Boolean.TRUE.equals(config.getSwipeEnabled())) {
                logger.info("Swipe integration is disabled");
                return null;
            }
            
            String apiKey = config.getSwipeApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Swipe API key not configured");
                return null;
            }
            
            // Get user
            User user = userRepository.findByEmail(order.getUserEmail())
                    .orElse(null);
            
            // Parse shipping address from JSON
            Map<String, Object> shippingAddressMap = new HashMap<>();
            if (order.getShippingAddress() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(
                        order.getShippingAddress(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                    shippingAddressMap = parsed;
                } catch (Exception e) {
                    logger.warn("Error parsing shipping address", e);
                }
            }
            
            // Create customer request
            SwipeDto.SwipeCustomerRequest customerRequest = new SwipeDto.SwipeCustomerRequest();
            customerRequest.setName(order.getUserName());
            customerRequest.setEmail(order.getUserEmail());
            
            if (user != null && user.getPhoneNumber() != null) {
                customerRequest.setPhone(user.getPhoneNumber());
            }
            
            // Set billing address from shipping address
            SwipeDto.SwipeAddressRequest billingAddress = new SwipeDto.SwipeAddressRequest();
            if (shippingAddressMap.containsKey("address")) {
                billingAddress.setAddress((String) shippingAddressMap.get("address"));
            }
            if (shippingAddressMap.containsKey("city")) {
                billingAddress.setCity((String) shippingAddressMap.get("city"));
            }
            if (shippingAddressMap.containsKey("state")) {
                billingAddress.setState((String) shippingAddressMap.get("state"));
            }
            if (shippingAddressMap.containsKey("zipCode")) {
                billingAddress.setPincode((String) shippingAddressMap.get("zipCode"));
            }
            customerRequest.setBillingAddress(billingAddress);
            
            // Set GSTIN if available
            if (shippingAddressMap.containsKey("gstin")) {
                String gstin = (String) shippingAddressMap.get("gstin");
                if (gstin != null && !gstin.trim().isEmpty()) {
                    customerRequest.setGstin(gstin);
                }
            }
            
            // Create invoice items
            List<SwipeDto.SwipeInvoiceItemRequest> items = new ArrayList<>();
            for (OrderItem orderItem : order.getItems()) {
                SwipeDto.SwipeInvoiceItemRequest item = new SwipeDto.SwipeInvoiceItemRequest();
                item.setName(orderItem.getName());
                item.setQuantity(orderItem.getQuantity());
                item.setUnitPrice(orderItem.getPrice());
                item.setTotalAmount(orderItem.getTotalPrice());
                
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
                
                // Calculate net amount
                if (gstRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    BigDecimal netAmount = orderItem.getTotalPrice().divide(divisor, 2, RoundingMode.HALF_UP);
                    item.setNetAmount(netAmount);
                } else {
                    item.setNetAmount(orderItem.getTotalPrice());
                }
                
                // Set HSN code
                if (product != null && product.getHsnCode() != null) {
                    item.setHsnCode(product.getHsnCode());
                } else {
                    item.setHsnCode("6109"); // Default HSN for garments
                }
                
                item.setItemType("goods");
                items.add(item);
            }
            
            // Create invoice request
            SwipeDto.SwipeInvoiceRequest invoiceRequest = new SwipeDto.SwipeInvoiceRequest();
            invoiceRequest.setDocumentType("invoice");
            invoiceRequest.setDocumentDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            invoiceRequest.setSerialNumber(order.getOrderNumber());
            invoiceRequest.setCustomer(customerRequest);
            invoiceRequest.setItems(items);
            invoiceRequest.setSubtotal(order.getSubtotal());
            invoiceRequest.setExtraDiscount(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO);
            invoiceRequest.setShipping(order.getShipping());
            invoiceRequest.setTotal(order.getTotal());
            invoiceRequest.setEinvoice(config.getEinvoiceEnabled() != null && config.getEinvoiceEnabled());
            
            // Call Swipe API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<SwipeDto.SwipeInvoiceRequest> request = new HttpEntity<>(invoiceRequest, headers);
            
            String invoiceUrl = swipeApiUrl + "/invoices";
            ResponseEntity<SwipeDto.SwipeInvoiceResponse> response = restTemplate.exchange(
                invoiceUrl,
                HttpMethod.POST,
                request,
                SwipeDto.SwipeInvoiceResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error creating invoice in Swipe for order: " + order.getId(), e);
            return null;
        }
    }
    
    /**
     * Get invoice details from Swipe
     */
    public SwipeDto.SwipeInvoiceResponse getInvoiceDetails(String invoiceId) {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            String apiKey = config.getSwipeApiKey();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return null;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String invoiceUrl = swipeApiUrl + "/invoices/" + invoiceId;
            ResponseEntity<SwipeDto.SwipeInvoiceResponse> response = restTemplate.exchange(
                invoiceUrl,
                HttpMethod.GET,
                request,
                SwipeDto.SwipeInvoiceResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error fetching invoice details from Swipe: " + invoiceId, e);
            return null;
        }
    }
}
