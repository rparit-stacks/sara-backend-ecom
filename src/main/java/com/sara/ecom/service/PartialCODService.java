package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.enums.PaymentGateway;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partial COD service
 * Handles advance payment online, remaining amount on delivery
 */
@Service
public class PartialCODService implements PaymentService {
    
    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.PARTIAL_COD;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // Partial COD advance payment is handled by Razorpay/Stripe
        // This service just confirms the order
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("order_id", request.getOrderId());
        orderData.put("order_number", request.getOrderNumber());
        orderData.put("payment_method", "PARTIAL_COD");
        
        return PaymentResponse.builder()
            .paymentId("PARTIAL_COD_" + request.getOrderId())
            .gateway("PARTIAL_COD")
            .status("PENDING")
            .orderData(orderData)
            .message("Order placed successfully. Advance payment will be processed online, remaining amount on delivery.")
            .build();
    }
    
    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        // Partial COD payment verification
        return PaymentResponse.builder()
            .paymentId(request.getPaymentId())
            .gateway("PARTIAL_COD")
            .status("PENDING")
            .message("Advance payment verified. Remaining amount will be collected on delivery")
            .build();
    }
    
    @Override
    public boolean isEnabled() {
        // Partial COD is enabled if configured in PaymentConfig
        // This is checked in PaymentServiceManager
        return true;
    }
    
    @Override
    public List<String> getAvailableMethods(String country) {
        return List.of("partial_cod");
    }
}
