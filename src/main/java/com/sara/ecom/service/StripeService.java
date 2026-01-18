package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.entity.BusinessConfig;
import com.sara.ecom.enums.PaymentGateway;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeService implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    private String getSecretKey() {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            return config != null ? config.getStripeSecretKey() : null;
        } catch (Exception e) {
            logger.error("Error getting Stripe secret key", e);
            return null;
        }
    }
    
    public String getPublicKey() {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            return config != null ? config.getStripePublicKey() : null;
        } catch (Exception e) {
            logger.error("Error getting Stripe public key", e);
            return null;
        }
    }
    
    private boolean isStripeEnabled() {
        try {
            BusinessConfig config = businessConfigService.getConfigEntity();
            if (config == null) return false;
            Boolean enabled = config.getStripeEnabled();
            String secretKey = config.getStripeSecretKey();
            return enabled != null && enabled && secretKey != null && !secretKey.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.STRIPE;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        String secretKey = getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new RuntimeException("Stripe is not configured. Please set API keys in admin panel.");
        }
        
        try {
            Stripe.apiKey = secretKey;
            
            // Convert amount to smallest currency unit (cents for USD, paise for INR)
            long amountInCents = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency().toLowerCase())
                .setDescription("Order #" + request.getOrderNumber())
                .putMetadata("order_id", String.valueOf(request.getOrderId()))
                .putMetadata("order_number", request.getOrderNumber())
                .setReceiptEmail(request.getCustomerEmail())
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("client_secret", paymentIntent.getClientSecret());
            orderData.put("payment_intent_id", paymentIntent.getId());
            
            return PaymentResponse.builder()
                .paymentId(paymentIntent.getId())
                .gateway("STRIPE")
                .status(paymentIntent.getStatus())
                .orderData(orderData)
                .message("Payment intent created successfully")
                .build();
                
        } catch (StripeException e) {
            logger.error("Stripe payment creation failed", e);
            throw new RuntimeException("Failed to create Stripe payment: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        String secretKey = getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new RuntimeException("Stripe is not configured");
        }
        
        try {
            Stripe.apiKey = secretKey;
            
            PaymentIntent paymentIntent = PaymentIntent.retrieve(request.getPaymentId());
            
            String status = "FAILED";
            if ("succeeded".equals(paymentIntent.getStatus())) {
                status = "SUCCESS";
            } else if ("requires_payment_method".equals(paymentIntent.getStatus()) || 
                       "canceled".equals(paymentIntent.getStatus())) {
                status = "FAILED";
            } else {
                status = "PENDING";
            }
            
            return PaymentResponse.builder()
                .paymentId(paymentIntent.getId())
                .gateway("STRIPE")
                .status(status)
                .message("Payment verification completed")
                .build();
                
        } catch (StripeException e) {
            logger.error("Stripe payment verification failed", e);
            throw new RuntimeException("Failed to verify Stripe payment: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isEnabled() {
        return isStripeEnabled();
    }
    
    @Override
    public List<String> getAvailableMethods(String country) {
        // Stripe supports card payments for all countries
        return List.of("card");
    }
}
