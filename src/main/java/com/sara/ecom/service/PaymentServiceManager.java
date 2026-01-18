package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.enums.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages multiple payment gateway services
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceManager {
    
    private final List<PaymentService> paymentServices;
    
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
        
        return getEnabledServices().stream()
            .filter(service -> {
                PaymentGateway gateway = service.getGateway();
                // COD is available for all countries
                if (gateway == PaymentGateway.COD) {
                    return true;
                }
                // Razorpay only for India
                if (gateway == PaymentGateway.RAZORPAY) {
                    return isIndia;
                }
                // Stripe for all countries
                if (gateway == PaymentGateway.STRIPE) {
                    return true;
                }
                return false;
            })
            .map(PaymentService::getGateway)
            .collect(Collectors.toList());
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
