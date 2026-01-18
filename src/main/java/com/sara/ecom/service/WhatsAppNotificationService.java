package com.sara.ecom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.OrderStatusTemplate;
import com.sara.ecom.entity.SentMessage;
import com.sara.ecom.entity.User;
import com.sara.ecom.repository.OrderStatusTemplateRepository;
import com.sara.ecom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class WhatsAppNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    
    @Autowired
    private OrderStatusTemplateRepository templateRepository;
    
    @Autowired
    private WASenderService wasenderService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Get order status template for a specific status type
     * Creates default template if not exists
     */
    public OrderStatusTemplate getOrderStatusTemplate(OrderStatusTemplate.StatusType statusType) {
        return templateRepository.findByStatusType(statusType)
                .orElseGet(() -> createDefaultTemplate(statusType));
    }
    
    /**
     * Create default template for a status type
     */
    private OrderStatusTemplate createDefaultTemplate(OrderStatusTemplate.StatusType statusType) {
        OrderStatusTemplate template = OrderStatusTemplate.builder()
                .statusType(statusType)
                .messageTemplate(getDefaultTemplateContent(statusType))
                .isEnabled(true) // Default enabled
                .build();
        return templateRepository.save(template);
    }
    
    /**
     * Get default template content for status type
     */
    private String getDefaultTemplateContent(OrderStatusTemplate.StatusType statusType) {
        return switch (statusType) {
            case ORDER_PLACED -> "Hello {{name}}, your order #{{order_id}} has been placed successfully. Total amount: {{amount}}. Thank you for shopping with us!";
            case ORDER_CONFIRMED -> "Hello {{name}}, your order #{{order_id}} has been confirmed. We're preparing your order for shipment.";
            case PAYMENT_SUCCESS -> "Hello {{name}}, payment of {{amount}} for order #{{order_id}} has been received successfully.";
            case PAYMENT_FAILED -> "Hello {{name}}, payment for order #{{order_id}} failed. Please try again or contact support.";
            case ORDER_SHIPPED -> "Hello {{name}}, your order #{{order_id}} has been shipped. We'll notify you once it's out for delivery.";
            case OUT_FOR_DELIVERY -> "Hello {{name}}, your order #{{order_id}} is out for delivery. Expected delivery: {{delivery_date}}";
            case DELIVERED -> "Hello {{name}}, your order #{{order_id}} has been delivered. Thank you for shopping with us!";
            case CANCELLED -> "Hello {{name}}, your order #{{order_id}} has been cancelled. If payment was made, refund will be processed within 5-7 business days.";
            case REFUND_INITIATED -> "Hello {{name}}, refund of {{amount}} for order #{{order_id}} has been initiated. It will reflect in your account within 5-7 business days.";
            case REFUND_COMPLETED -> "Hello {{name}}, refund of {{amount}} for order #{{order_id}} has been completed. Please check your account.";
        };
    }
    
    /**
     * Send order status notification
     */
    @Transactional
    public void sendOrderStatusNotification(Order order, OrderStatusTemplate.StatusType statusType) {
        // Get template for this status (will create default if not exists)
        OrderStatusTemplate template = getOrderStatusTemplate(statusType);
        
        if (template == null || !template.getIsEnabled()) {
            return; // Template disabled
        }
        
        // Get user (for name and other details)
        User user = userRepository.findByEmail(order.getUserEmail()).orElse(null);
        if (user == null) {
            logger.warn("Cannot send WhatsApp notification: User {} not found", order.getUserEmail());
            return; // User not found
        }
        
        // Get phone number from order's shipping address (single source of truth)
        String phoneNumber = null;
        if (order.getShippingAddress() != null && !order.getShippingAddress().trim().isEmpty()) {
            try {
                Map<String, Object> shippingAddressMap = objectMapper.readValue(
                    order.getShippingAddress(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                // Try different possible keys for phone number
                phoneNumber = (String) shippingAddressMap.getOrDefault("phone", 
                    shippingAddressMap.getOrDefault("phoneNumber", null));
            } catch (Exception e) {
                logger.warn("Failed to parse shipping address JSON for order {}: {}", order.getId(), e.getMessage());
            }
        }
        
        // Fallback to user's phone number if not found in shipping address
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            phoneNumber = user.getPhoneNumber();
            logger.debug("Using user profile phone number as fallback for order {}", order.getId());
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.warn("Cannot send WhatsApp notification: No phone number found in order {} shipping address or user profile", order.getId());
            return; // No phone number
        }
        
        // Format phone number (remove spaces, +, etc. - keep only digits)
        phoneNumber = formatPhoneNumber(phoneNumber);
        if (phoneNumber == null || phoneNumber.length() < 10) {
            logger.warn("Cannot send WhatsApp notification: Invalid phone number format '{}' for order {}", 
                phoneNumber, order.getId());
            return;
        }
        
        // Replace variables in template
        String message = replaceVariables(template.getMessageTemplate(), order, user);
        if (message == null || message.trim().isEmpty()) {
            logger.warn("Cannot send WhatsApp notification: Empty message template for status {}", statusType);
            return;
        }
        
        // Send message
        try {
            logger.info("Sending WhatsApp notification to {} for order {} with status {}", phoneNumber, order.getId(), statusType);
            wasenderService.sendMessage(phoneNumber, message, SentMessage.MessageType.ORDER_NOTIFICATION, order.getId());
            logger.info("Successfully sent WhatsApp notification to {} for order {}", phoneNumber, order.getId());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp order notification to {} for order {}: {}", 
                phoneNumber, order.getId(), e.getMessage(), e);
            // Don't throw - just log the error
        }
    }
    
    /**
     * Replace variables in template with actual values
     */
    public String replaceVariables(String template, Order order, User user) {
        if (template == null) {
            return "";
        }
        
        String message = template;
        
        // Order variables
        message = message.replace("{{order_id}}", order.getOrderNumber() != null ? order.getOrderNumber() : "");
        message = message.replace("{{name}}", getUserName(user));
        message = message.replace("{{amount}}", formatCurrency(order.getTotal()));
        message = message.replace("{{status}}", order.getStatus() != null ? order.getStatus().name() : "");
        message = message.replace("{{payment_status}}", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "");
        
        // Items count
        int itemsCount = order.getItems() != null ? order.getItems().size() : 0;
        message = message.replace("{{items_count}}", String.valueOf(itemsCount));
        
        // Tracking number (if available - you may need to add this field to Order entity)
        message = message.replace("{{tracking_number}}", ""); // Placeholder
        
        // Delivery date (if available)
        message = message.replace("{{delivery_date}}", ""); // Placeholder
        
        // Order date
        if (order.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            message = message.replace("{{order_date}}", order.getCreatedAt().format(formatter));
        } else {
            message = message.replace("{{order_date}}", "");
        }
        
        // Subtotal, GST, Shipping
        message = message.replace("{{subtotal}}", formatCurrency(order.getSubtotal()));
        message = message.replace("{{gst}}", formatCurrency(order.getGst()));
        message = message.replace("{{shipping}}", formatCurrency(order.getShipping()));
        
        // Coupon code
        if (order.getCouponCode() != null) {
            message = message.replace("{{coupon_code}}", order.getCouponCode());
        } else {
            message = message.replace("{{coupon_code}}", "");
        }
        
        return message;
    }
    
    /**
     * Preview template with sample data
     */
    public String previewTemplate(OrderStatusTemplate.StatusType statusType) {
        OrderStatusTemplate template = getOrderStatusTemplate(statusType);
        if (template == null) {
            return "Template not found";
        }
        
        // Create sample order and user for preview
        Order sampleOrder = new Order();
        sampleOrder.setOrderNumber("1234567");
        sampleOrder.setStatus(Order.OrderStatus.PENDING);
        sampleOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
        sampleOrder.setTotal(BigDecimal.valueOf(1500.00));
        sampleOrder.setSubtotal(BigDecimal.valueOf(1300.00));
        sampleOrder.setGst(BigDecimal.valueOf(200.00));
        sampleOrder.setShipping(BigDecimal.valueOf(0.00));
        sampleOrder.setCreatedAt(LocalDateTime.now());
        
        User sampleUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+919876543210")
                .build();
        
        return replaceVariables(template.getMessageTemplate(), sampleOrder, sampleUser);
    }
    
    private String getUserName(User user) {
        if (user == null) {
            return "Customer";
        }
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        return "₹" + amount.toString();
    }
    
    /**
     * Format phone number for WhatsApp API
     * Removes spaces, +, -, and other non-digit characters
     * Keeps only digits (should be 10-15 digits)
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        
        // Remove leading country code if present (91 for India)
        if (cleaned.length() > 10 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2);
        }
        
        // Validate length (should be 10 digits for Indian numbers)
        if (cleaned.length() < 10 || cleaned.length() > 15) {
            logger.warn("Phone number format invalid after cleaning: {} (original: {})", cleaned, phoneNumber);
            return null;
        }
        
        return cleaned;
    }
}
