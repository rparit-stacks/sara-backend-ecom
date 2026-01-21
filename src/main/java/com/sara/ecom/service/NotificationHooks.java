package com.sara.ecom.service;

import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.User;
import com.sara.ecom.entity.WhatsAppTemplate;
import com.sara.ecom.repository.UserRepository;
import com.sara.ecom.repository.WhatsAppTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles notification integrations (WhatsApp, SMS, push).
 */
@Component
public class NotificationHooks {

    @Autowired
    private WhatsAppService whatsAppService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WhatsAppTemplateRepository templateRepository;

    public void onOrderPlaced(Order order) {
        // WhatsApp notification for order placed can be added here if needed
    }

    public void onOrderStatusChanged(Order order, String oldStatus, String newStatus, String customMessage, boolean skipWhatsApp) {
        if (skipWhatsApp) {
            return;
        }
        
        try {
            User user = userRepository.findByEmail(order.getUserEmail()).orElse(null);
            
            // Determine message to send
            String message = null;
            
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                // Use custom message provided
                message = customMessage;
            } else {
                // Try to find template for the status
                String statusType = newStatus;
                if (order.getCustomStatus() != null && !order.getCustomStatus().trim().isEmpty()) {
                    statusType = order.getCustomStatus();
                }
                
                WhatsAppTemplate template = templateRepository.findByStatusType(statusType).orElse(null);
                if (template != null && template.getIsEnabled() != null && template.getIsEnabled()) {
                    // Use template
                    message = whatsAppService.replaceVariables(template.getMessageTemplate(), order, user, null);
                } else {
                    // Fallback: try standard status template
                    template = templateRepository.findByStatusType(newStatus).orElse(null);
                    if (template != null && template.getIsEnabled() != null && template.getIsEnabled()) {
                        message = whatsAppService.replaceVariables(template.getMessageTemplate(), order, user, null);
                    }
                }
            }
            
            // If we have a message, send it
            if (message != null && !message.trim().isEmpty()) {
                whatsAppService.sendOrderStatusNotification(order, user, message);
            }
        } catch (Exception e) {
            // Log error but don't fail order update
            System.err.println("Failed to send WhatsApp notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPaymentStatusChanged(Order order) {
        // WhatsApp notification for payment status can be added here if needed
    }
}

