package com.sara.ecom.controller;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.enums.PaymentGateway;
import com.sara.ecom.service.PaymentServiceManager;
import com.sara.ecom.service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentServiceManager paymentServiceManager;
    
    @Autowired
    private StripeService stripeService;
    
    /**
     * Get available payment methods for a country
     */
    @GetMapping("/methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethods(@RequestParam String country) {
        List<PaymentGateway> gateways = paymentServiceManager.getAvailableGateways(country);
        
        Map<String, Object> response = new HashMap<>();
        response.put("country", country);
        response.put("gateways", gateways.stream().map(Enum::name).collect(Collectors.toList()));
        
        // Get available methods for each gateway
        Map<String, List<String>> methods = new HashMap<>();
        for (PaymentGateway gateway : gateways) {
            var service = paymentServiceManager.getService(gateway);
            methods.put(gateway.name(), service.getAvailableMethods(country));
        }
        response.put("methods", methods);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a payment order
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createPaymentOrder(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentServiceManager.createPayment(request);
        
        // Add Stripe public key to response if Stripe is being used
        if ("STRIPE".equalsIgnoreCase(request.getPaymentGateway()) && response.getGateway().equals("STRIPE")) {
            String publicKey = stripeService.getPublicKey();
            if (publicKey != null) {
                response.getOrderData().put("key_id", publicKey);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify a payment
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentServiceManager.verifyPayment(request);
        return ResponseEntity.ok(response);
    }
}
