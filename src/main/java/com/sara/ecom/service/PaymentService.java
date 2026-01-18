package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.enums.PaymentGateway;

import java.util.List;

/**
 * Abstract payment service interface
 * Implementations: StripeService, RazorpayService
 */
public interface PaymentService {
    
    /**
     * Get the payment gateway this service handles
     */
    PaymentGateway getGateway();
    
    /**
     * Create a payment order
     */
    PaymentResponse createPayment(PaymentRequest request);
    
    /**
     * Verify a payment
     */
    PaymentResponse verifyPayment(PaymentVerificationRequest request);
    
    /**
     * Check if this gateway is enabled
     */
    boolean isEnabled();
    
    /**
     * Get available payment methods for a country
     */
    List<String> getAvailableMethods(String country);
}
