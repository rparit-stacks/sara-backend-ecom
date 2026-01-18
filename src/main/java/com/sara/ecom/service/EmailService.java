package com.sara.ecom.service;

import com.sara.ecom.dto.EmailTemplateData;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Studio Sara}")
    private String appName;
    
    @Value("${app.url:https://www.studiosara.in}")
    private String appUrl;
    
    @Value("${app.email:info@codvertex.in}")
    private String appEmail;
    
    @Value("${app.phone:+91-XXXXXXXXXX}")
    private String appPhone;
    
    @Value("${app.address:}")
    private String appAddress;
    
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your OTP for Login");
        message.setText("Your OTP is: " + otp + "\n\nThis OTP will expire in 5 minutes.");
        
        mailSender.send(message);
    }
    
    private void sendHtmlEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Add common variables
            Map<String, Object> templateVariables = new HashMap<>(variables);
            templateVariables.put("appName", appName);
            templateVariables.put("appUrl", appUrl);
            templateVariables.put("appEmail", appEmail);
            templateVariables.put("appPhone", appPhone);
            templateVariables.put("appAddress", appAddress);
            templateVariables.put("currentYear", LocalDateTime.now().getYear());
            
            Context context = new Context();
            context.setVariables(templateVariables);
            
            String htmlContent = templateEngine.process("emails/" + templateName, context);
            
            try {
                helper.setFrom(fromEmail, appName);
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Order emails
    public void sendOrderPlacedEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Placed - " + data.getOrderNumber(), "order-placed", variables);
    }
    
    public void sendOrderConfirmedEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Confirmed - " + data.getOrderNumber(), "order-confirmed", variables);
    }
    
    public void sendOrderProcessingEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Processing - " + data.getOrderNumber(), "order-processing", variables);
    }
    
    public void sendOrderShippedEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Shipped - " + data.getOrderNumber(), "order-shipped", variables);
    }
    
    public void sendOrderDeliveredEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Delivered - " + data.getOrderNumber(), "order-delivered", variables);
    }
    
    public void sendOrderCancelledEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Order Cancelled - " + data.getOrderNumber(), "order-cancelled", variables);
    }
    
    public void sendPaymentPendingEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Payment Pending - " + data.getOrderNumber(), "payment-pending", variables);
    }
    
    public void sendPaymentSuccessfulEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Payment Successful - " + data.getOrderNumber(), "payment-successful", variables);
    }
    
    public void sendPaymentFailedEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Payment Failed - " + data.getOrderNumber(), "payment-failed", variables);
    }
    
    public void sendPaymentRefundedEmail(EmailTemplateData.OrderEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Payment Refunded - " + data.getOrderNumber(), "payment-refunded", variables);
    }
    
    // Cart email
    public void sendItemAddedToCartEmail(EmailTemplateData.CartEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Item Added to Your Cart", "item-added-to-cart", variables);
    }
    
    // Wishlist email
    public void sendItemAddedToWishlistEmail(EmailTemplateData.WishlistEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Item Added to Your Wishlist", "item-added-to-wishlist", variables);
    }
    
    // Welcome email
    public void sendWelcomeEmail(EmailTemplateData.WelcomeEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Welcome to " + appName, "welcome", variables);
    }
    
    // Login notification
    public void sendLoginNotificationEmail(EmailTemplateData.LoginNotificationData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "New Login Detected", "login-notification", variables);
    }
    
    // Design request emails
    public void sendDesignRequestSubmittedEmail(EmailTemplateData.DesignRequestEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Custom Design Request Submitted", "design-request-submitted", variables);
    }
    
    public void sendDesignRequestStatusUpdatedEmail(EmailTemplateData.DesignRequestEmailData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Customer");
        sendHtmlEmail(data.getRecipientEmail(), "Design Request Status Updated", "design-request-status-updated", variables);
    }
    
    // Newsletter subscription
    public void sendNewsletterSubscriptionEmail(EmailTemplateData.NewsletterSubscriptionData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        variables.put("recipientName", data.getRecipientName() != null ? data.getRecipientName() : "Subscriber");
        sendHtmlEmail(data.getRecipientEmail(), "Welcome to Our Newsletter", "newsletter-subscription", variables);
    }
    
    // Contact form (to admin)
    public void sendContactFormEmail(EmailTemplateData.ContactFormData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", data);
        sendHtmlEmail(appEmail, "New Contact Form Submission from " + data.getSenderName(), "contact-form", variables);
    }
}
