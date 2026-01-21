package com.sara.ecom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.entity.BusinessConfig;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.User;
import com.sara.ecom.entity.WhatsAppNotificationLog;
import com.sara.ecom.repository.WhatsAppNotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class WhatsAppService {
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    @Autowired
    private WhatsAppNotificationLogRepository notificationLogRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOUBLETICK_TEXT_API_URL = "https://public.doubletick.io/whatsapp/message/text";
    private static final String DOUBLETICK_TEMPLATE_API_URL = "https://public.doubletick.io/whatsapp/message/template";
    
    /**
     * Sends a text message via DoubleTick API
     */
    public WhatsAppNotificationLog sendTextMessage(String phoneNumber, String message) {
        return sendTextMessage(null, phoneNumber, message);
    }
    
    public WhatsAppNotificationLog sendTextMessage(Long orderId, String phoneNumber, String message) {
        BusinessConfig config = businessConfigService.getConfigEntity();
        
        if (config.getDoubletickEnabled() == null || !config.getDoubletickEnabled()) {
            return createLogEntry(orderId, phoneNumber, message, "FAILED", null, "WhatsApp notifications are disabled");
        }
        
        String apiKey = config.getDoubletickApiKey();
        String senderNumber = config.getDoubletickSenderNumber();
        
        if (apiKey == null || apiKey.trim().isEmpty() || senderNumber == null || senderNumber.trim().isEmpty()) {
            return createLogEntry(orderId, phoneNumber, message, "FAILED", null, "DoubleTick API key or sender number not configured");
        }
        
        // Format phone number to international format
        String formattedPhone = formatPhoneNumber(phoneNumber);
        if (formattedPhone == null) {
            return createLogEntry(orderId, phoneNumber, message, "FAILED", null, "Invalid phone number format");
        }
        
        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", formattedPhone);
            requestBody.put("from", senderNumber);
            requestBody.put("messageId", UUID.randomUUID().toString());
            
            Map<String, Object> content = new HashMap<>();
            content.put("text", message);
            requestBody.put("content", content);
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            System.out.println("[WhatsAppService] Sending message to DoubleTick API:");
            System.out.println("[WhatsAppService] URL: " + DOUBLETICK_TEXT_API_URL);
            System.out.println("[WhatsAppService] To: " + formattedPhone);
            System.out.println("[WhatsAppService] From: " + senderNumber);
            System.out.println("[WhatsAppService] Message: " + message);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                DOUBLETICK_TEXT_API_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            System.out.println("[WhatsAppService] Response Status: " + response.getStatusCode());
            System.out.println("[WhatsAppService] Response Body: " + response.getBody());
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                String status = (String) responseBody.get("status");
                
                System.out.println("[WhatsAppService] Message sent successfully. MessageId: " + messageId + ", Status: " + status);
                return createLogEntry(orderId, formattedPhone, message, status != null ? status : "SENT", messageId, null);
            } else {
                String errorMsg = "Unexpected response from DoubleTick API. Status: " + response.getStatusCode() + ", Body: " + response.getBody();
                System.out.println("[WhatsAppService] ERROR: " + errorMsg);
                return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorMsg);
            }
            
        } catch (HttpClientErrorException e) {
            // Handle 4xx errors (400, 401, 422, etc.)
            String errorDetails = "HTTP " + e.getStatusCode().value() + " Error";
            String responseBody = e.getResponseBodyAsString();
            
            System.out.println("[WhatsAppService] HTTP Client Error:");
            System.out.println("[WhatsAppService] Status Code: " + e.getStatusCode());
            System.out.println("[WhatsAppService] Response Body: " + responseBody);
            System.out.println("[WhatsAppService] Headers: " + e.getResponseHeaders());
            
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    // Try to parse error message from response
                    Map<String, Object> errorResponse = objectMapper.readValue(responseBody, 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    String errorMessage = (String) errorResponse.get("message");
                    String error = (String) errorResponse.get("error");
                    if (errorMessage != null) {
                        errorDetails = errorDetails + ": " + errorMessage;
                    } else if (error != null) {
                        errorDetails = errorDetails + ": " + error;
                    } else {
                        errorDetails = errorDetails + ": " + responseBody;
                    }
                } catch (Exception parseEx) {
                    errorDetails = errorDetails + ": " + responseBody;
                }
            } else {
                errorDetails = errorDetails + ": " + e.getMessage();
            }
            
            // If chat window is closed, try template message as fallback
            if (responseBody != null && responseBody.contains("Chat window is closed")) {
                System.out.println("[WhatsAppService] Chat window closed, trying template message fallback...");
                try {
                    return sendTemplateMessageFallback(orderId, formattedPhone, message, apiKey, senderNumber);
                } catch (Exception fallbackEx) {
                    System.out.println("[WhatsAppService] Template fallback also failed: " + fallbackEx.getMessage());
                    return createLogEntry(orderId, formattedPhone, message, "FAILED", null, 
                        errorDetails + " | Template fallback failed: " + fallbackEx.getMessage());
                }
            }
            
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorDetails);
            
        } catch (HttpServerErrorException e) {
            // Handle 5xx errors
            String errorDetails = "HTTP " + e.getStatusCode().value() + " Server Error: " + e.getResponseBodyAsString();
            System.out.println("[WhatsAppService] HTTP Server Error:");
            System.out.println("[WhatsAppService] Status Code: " + e.getStatusCode());
            System.out.println("[WhatsAppService] Response Body: " + e.getResponseBodyAsString());
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorDetails);
            
        } catch (RestClientException e) {
            // Handle other REST client errors (network issues, etc.)
            String errorMessage = "Network/Connection Error: " + e.getMessage();
            System.out.println("[WhatsAppService] RestClientException:");
            System.out.println("[WhatsAppService] Error: " + e.getMessage());
            e.printStackTrace();
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorMessage);
            
        } catch (Exception e) {
            // Handle any other unexpected errors
            String errorMessage = "Unexpected Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            System.out.println("[WhatsAppService] Unexpected Exception:");
            System.out.println("[WhatsAppService] Type: " + e.getClass().getName());
            System.out.println("[WhatsAppService] Message: " + e.getMessage());
            e.printStackTrace();
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorMessage);
        }
    }
    
    /**
     * Sends template message as fallback when chat window is closed
     */
    private WhatsAppNotificationLog sendTemplateMessageFallback(Long orderId, String formattedPhone, 
                                                                 String message, String apiKey, String senderNumber) {
        try {
            System.out.println("[WhatsAppService] Sending template message fallback:");
            System.out.println("[WhatsAppService] To: " + formattedPhone);
            System.out.println("[WhatsAppService] Message: " + message);
            
            // Get template name from config
            BusinessConfig config = businessConfigService.getConfigEntity();
            String templateName = config.getDoubletickTemplateName();
            if (templateName == null || templateName.trim().isEmpty()) {
                templateName = "order_status_update"; // Default template name
            }
            System.out.println("[WhatsAppService] Using template: " + templateName);
            
            // Prepare template message request
            // Using simple format: message in body placeholders
            Map<String, Object> messageObj = new HashMap<>();
            messageObj.put("to", formattedPhone);
            messageObj.put("from", senderNumber);
            messageObj.put("messageId", UUID.randomUUID().toString());
            
            Map<String, Object> content = new HashMap<>();
            content.put("language", "en");
            content.put("templateName", templateName);
            
            Map<String, Object> templateData = new HashMap<>();
            Map<String, Object> body = new HashMap<>();
            // Split message into placeholders if it's long, otherwise use single placeholder
            List<String> placeholders = new ArrayList<>();
            if (message.length() > 100) {
                // Split into chunks
                int chunkSize = 100;
                for (int i = 0; i < message.length(); i += chunkSize) {
                    placeholders.add(message.substring(i, Math.min(i + chunkSize, message.length())));
                }
            } else {
                placeholders.add(message);
            }
            body.put("placeholders", placeholders);
            templateData.put("body", body);
            content.put("templateData", templateData);
            messageObj.put("content", content);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(messageObj);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messages);
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                DOUBLETICK_TEMPLATE_API_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            System.out.println("[WhatsAppService] Template Response Status: " + response.getStatusCode());
            System.out.println("[WhatsAppService] Template Response Body: " + response.getBody());
            
            // Template API returns 200 OK (not 201) and has "messages" array in response
            if ((response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) 
                && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = null;
                String status = null;
                
                // Check if response has "messages" array
                if (responseBody.containsKey("messages")) {
                    Object messagesObj = responseBody.get("messages");
                    if (messagesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> responseMessages = (List<Map<String, Object>>) messagesObj;
                        if (responseMessages != null && !responseMessages.isEmpty()) {
                            Map<String, Object> firstMessage = responseMessages.get(0);
                            messageId = (String) firstMessage.get("messageId");
                            status = (String) firstMessage.get("status");
                        }
                    }
                } else if (responseBody.containsKey("messageId")) {
                    // Direct response format
                    messageId = (String) responseBody.get("messageId");
                    status = (String) responseBody.get("status");
                }
                
                System.out.println("[WhatsAppService] Template message sent successfully. MessageId: " + messageId + ", Status: " + status);
                return createLogEntry(orderId, formattedPhone, message, status != null ? status : "ENQUEUED", messageId, null);
            }
            
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, "Template message fallback failed");
            
        } catch (Exception e) {
            String errorMessage = "Template fallback error: " + e.getMessage();
            System.out.println("[WhatsAppService] Template fallback exception: " + errorMessage);
            e.printStackTrace();
            return createLogEntry(orderId, formattedPhone, message, "FAILED", null, errorMessage);
        }
    }
    
    /**
     * Replaces variables in template with order and user data
     */
    public String replaceVariables(String template, Order order, User user, String customMessage) {
        if (template == null) {
            return "";
        }
        
        String result = template;
        
        // Replace order variables
        if (order != null) {
            result = result.replace("{{order_id}}", order.getOrderNumber() != null ? order.getOrderNumber() : "");
            result = result.replace("{{amount}}", order.getTotal() != null ? "₹" + order.getTotal().toString() : "");
            result = result.replace("{{status}}", order.getStatus() != null ? order.getStatus().name() : "");
            result = result.replace("{{custom_status}}", order.getCustomStatus() != null ? order.getCustomStatus() : "");
            result = result.replace("{{subtotal}}", order.getSubtotal() != null ? "₹" + order.getSubtotal().toString() : "");
            result = result.replace("{{gst}}", order.getGst() != null ? "₹" + order.getGst().toString() : "");
            result = result.replace("{{shipping}}", order.getShipping() != null ? "₹" + order.getShipping().toString() : "");
            result = result.replace("{{coupon_code}}", order.getCouponCode() != null ? order.getCouponCode() : "");
            result = result.replace("{{coupon_discount}}", order.getCouponDiscount() != null ? "₹" + order.getCouponDiscount().toString() : "");
            
            // Format order date
            if (order.getCreatedAt() != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
                result = result.replace("{{order_date}}", order.getCreatedAt().format(formatter));
            } else {
                result = result.replace("{{order_date}}", "");
            }
            
            // Items count
            if (order.getItems() != null) {
                int itemsCount = order.getItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
                result = result.replace("{{items_count}}", String.valueOf(itemsCount));
            } else {
                result = result.replace("{{items_count}}", "0");
            }
        }
        
        // Replace user variables
        if (user != null) {
            String name = "";
            if (user.getFirstName() != null) {
                name = user.getFirstName();
            }
            if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                name += (name.isEmpty() ? "" : " ") + user.getLastName();
            }
            result = result.replace("{{name}}", name.isEmpty() ? (order != null && order.getUserName() != null ? order.getUserName() : "") : name);
        } else if (order != null && order.getUserName() != null) {
            result = result.replace("{{name}}", order.getUserName());
        } else {
            result = result.replace("{{name}}", "");
        }
        
        // Replace custom message variable
        result = result.replace("{{custom_message}}", customMessage != null ? customMessage : "");
        
        // Replace payment status
        if (order != null && order.getPaymentStatus() != null) {
            result = result.replace("{{payment_status}}", order.getPaymentStatus().name());
        } else {
            result = result.replace("{{payment_status}}", "");
        }
        
        return result;
    }
    
    /**
     * Formats phone number to international format (e.g., 919876543210)
     * Handles various formats: +91 9876543210, 91 9876543210, 9876543210, etc.
     */
    public String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        // Remove all spaces, dashes, and parentheses
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Remove leading + if present
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        
        // If starts with 91 and has 12 digits total, it's already in correct format
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return cleaned;
        }
        
        // If it's 10 digits, assume it's Indian number and add 91
        if (cleaned.length() == 10 && cleaned.matches("\\d+")) {
            return "91" + cleaned;
        }
        
        // If it's 11 digits and starts with 0, remove 0 and add 91
        if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            return "91" + cleaned.substring(1);
        }
        
        // If already 12 digits and starts with 91, return as is
        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            return cleaned;
        }
        
        // If it matches the pattern, return as is
        if (cleaned.matches("^91\\d{10}$")) {
            return cleaned;
        }
        
        // If all else fails, return cleaned number (might be international format already)
        return cleaned;
    }
    
    /**
     * Creates a log entry for WhatsApp notification
     */
    private WhatsAppNotificationLog createLogEntry(Long orderId, String phoneNumber, String message, 
                                                   String deliveryStatus, String messageId, String errorMessage) {
        WhatsAppNotificationLog log = new WhatsAppNotificationLog();
        log.setOrderId(orderId);
        log.setPhoneNumber(phoneNumber);
        log.setMessage(message);
        log.setDeliveryStatus(deliveryStatus);
        log.setMessageId(messageId);
        log.setErrorMessage(errorMessage);
        return notificationLogRepository.save(log);
    }
    
    /**
     * Sends notification for order status change
     */
    public WhatsAppNotificationLog sendOrderStatusNotification(Order order, User user, String message) {
        // Get phone number from shipping address or user
        String phoneNumber = null;
        
        if (order.getShippingAddress() != null) {
            try {
                Map<String, Object> shippingAddress = objectMapper.readValue(
                    order.getShippingAddress(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                phoneNumber = (String) shippingAddress.get("phone");
                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                    phoneNumber = (String) shippingAddress.get("phoneNumber");
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        // Fallback to user phone number
        if ((phoneNumber == null || phoneNumber.trim().isEmpty()) && user != null) {
            phoneNumber = user.getPhoneNumber();
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            WhatsAppNotificationLog log = createLogEntry(order.getId(), null, message, "FAILED", null, "Phone number not found");
            return log;
        }
        
        return sendTextMessage(order.getId(), phoneNumber, message);
    }
}
