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
 * Cash on Delivery service
 * No actual payment processing, just order confirmation
 */
@Service
public class CODService implements PaymentService {
    
    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.COD;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // COD doesn't require payment creation
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("order_id", request.getOrderId());
        orderData.put("order_number", request.getOrderNumber());
        orderData.put("payment_method", "COD");
        
        return PaymentResponse.builder()
            .paymentId("COD_" + request.getOrderId())
            .gateway("COD")
            .status("PENDING")
            .orderData(orderData)
            .message("Order placed successfully. Payment will be collected on delivery.")
            .build();
    }
    
    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        // COD payment is always pending until delivery
        return PaymentResponse.builder()
            .paymentId(request.getPaymentId())
            .gateway("COD")
            .status("PENDING")
            .message("Payment will be collected on delivery")
            .build();
    }
    
    @Override
    public boolean isEnabled() {
        // COD is always enabled
        return true;
    }
    
    @Override
    public List<String> getAvailableMethods(String country) {
        // COD is available for all countries
        return List.of("cod");
    }
}
