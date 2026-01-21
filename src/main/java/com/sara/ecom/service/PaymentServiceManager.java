package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.entity.PaymentConfig;
import com.sara.ecom.enums.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages multiple payment gateway services
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceManager {
    
    private final List<PaymentService> paymentServices;
    
    @Autowired
    private PaymentConfigService paymentConfigService;
    
    /**
     * Get payment service for a specific gateway
     */
    public PaymentService getService(PaymentGateway gateway) {
        return paymentServices.stream()
            .filter(service -> service.getGateway() == gateway)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Payment gateway not found: " + gateway));
    }
    
    /**
     * Get all enabled payment services
     */
    public List<PaymentService> getEnabledServices() {
        return paymentServices.stream()
            .filter(PaymentService::isEnabled)
            .collect(Collectors.toList());
    }
    
    /**
     * Get available payment gateways for a country
     */
    public List<PaymentGateway> getAvailableGateways(String country) {
        boolean isIndia = "India".equalsIgnoreCase(country) || "IN".equalsIgnoreCase(country);
        PaymentConfig config = paymentConfigService.getConfigEntity();
        
        List<PaymentGateway> gateways = new ArrayList<>();
        
        // Online payment gateways
        if (isIndia && config.getRazorpayEnabled() != null && config.getRazorpayEnabled()) {
            // Check if Razorpay service is enabled (has keys)
            PaymentService razorpayService = paymentServices.stream()
                .filter(s -> s.getGateway() == PaymentGateway.RAZORPAY)
                .findFirst()
                .orElse(null);
            if (razorpayService != null && razorpayService.isEnabled()) {
                gateways.add(PaymentGateway.RAZORPAY);
            }
        }
        
        if (config.getStripeEnabled() != null && config.getStripeEnabled()) {
            // Check if Stripe service is enabled (has keys)
            PaymentService stripeService = paymentServices.stream()
                .filter(s -> s.getGateway() == PaymentGateway.STRIPE)
                .findFirst()
                .orElse(null);
            if (stripeService != null && stripeService.isEnabled()) {
                gateways.add(PaymentGateway.STRIPE);
            }
        }
        
        // COD and Partial COD based on country and config
        if (isIndia) {
            // India: COD and Partial COD available if enabled
            if (config.getCodEnabled() != null && config.getCodEnabled()) {
                gateways.add(PaymentGateway.COD);
            }
            if (config.getPartialCodEnabled() != null && config.getPartialCodEnabled()) {
                // Partial COD requires at least one gateway (Razorpay or Stripe)
                boolean hasGateway = gateways.contains(PaymentGateway.RAZORPAY) || gateways.contains(PaymentGateway.STRIPE);
                if (hasGateway) {
                    gateways.add(PaymentGateway.PARTIAL_COD);
                }
            }
        } else {
            // Outside India: Only Partial COD if enabled (COD not available)
            if (config.getPartialCodEnabled() != null && config.getPartialCodEnabled()) {
                // Partial COD requires at least one gateway (Stripe for outside India)
                boolean hasStripe = gateways.contains(PaymentGateway.STRIPE);
                if (hasStripe) {
                    gateways.add(PaymentGateway.PARTIAL_COD);
                }
            }
        }
        
        return gateways;
    }
    
    /**
     * Create payment using the specified gateway
     */
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentGateway gateway = PaymentGateway.valueOf(request.getPaymentGateway().toUpperCase());
        PaymentService service = getService(gateway);
        
        if (!service.isEnabled()) {
            throw new RuntimeException("Payment gateway is not enabled: " + gateway);
        }
        
        return service.createPayment(request);
    }
    
    /**
     * Verify payment using the specified gateway
     */
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        PaymentGateway gateway = PaymentGateway.valueOf(request.getGateway().toUpperCase());
        PaymentService service = getService(gateway);
        
        return service.verifyPayment(request);
    }
}
