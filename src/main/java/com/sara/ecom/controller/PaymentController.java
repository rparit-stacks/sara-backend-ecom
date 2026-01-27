package com.sara.ecom.controller;

import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.entity.Order;
import com.sara.ecom.enums.PaymentGateway;
import com.sara.ecom.repository.OrderRepository;
import com.sara.ecom.service.OrderService;
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
    private final OrderService orderService;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private OrderRepository orderRepository;
    
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

        // Persist payment currency and amount on the Order so admin can see them
        if (request.getOrderId() != null && request.getAmount() != null && request.getCurrency() != null) {
            try {
                Order order = orderRepository.findById(request.getOrderId()).orElse(null);
                if (order != null) {
                    order.setPaymentCurrency(request.getCurrency().toUpperCase());
                    order.setPaymentAmount(request.getAmount());
                    orderRepository.save(order);
                }
            } catch (Exception e) {
                // Log but do not fail payment creation
                System.err.println("Failed to persist payment currency/amount on order " +
                        request.getOrderId() + ": " + e.getMessage());
            }
        }
        
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
     * Verify a payment. On success, updates the order's payment status to PAID so post-payment
     * flow (confirmation, downloads) reflects the correct state immediately.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        // #region agent log
        try (java.io.FileWriter fw = new java.io.FileWriter("r:\\My Projects\\Quout\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"pre-fix-run1\",\"hypothesisId\":\"H1\",\"location\":\"PaymentController.verifyPayment:before\",\"message\":\"verifyPayment called\",\"data\":{\"gateway\":\""
                    + (request.getGateway() != null ? request.getGateway() : "")
                    + "\",\"orderId\":\""
                    + (request.getOrderId() != null ? request.getOrderId() : "")
                    + "\"},\"timestamp\":"
                    + System.currentTimeMillis()
                    + "}\n");
        } catch (Exception ignored) {}
        // #endregion

        PaymentResponse response = paymentServiceManager.verifyPayment(request);
        if (request.getOrderId() != null && !request.getOrderId().isBlank()) {
            try {
                Long orderId = Long.parseLong(request.getOrderId().trim());
                String paymentId = response.getPaymentId() != null ? response.getPaymentId() : "verified";
                if ("SUCCESS".equals(response.getStatus())) {
                    orderService.updatePaymentStatus(orderId, "PAID", paymentId);
                } else if ("FAILED".equals(response.getStatus())) {
                    orderService.updatePaymentStatus(orderId, "FAILED", paymentId);
                }
                // #region agent log
                try (java.io.FileWriter fw = new java.io.FileWriter("r:\\My Projects\\Quout\\.cursor\\debug.log", true)) {
                    fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"pre-fix-run1\",\"hypothesisId\":\"H1\",\"location\":\"PaymentController.verifyPayment:afterUpdate\",\"message\":\"verifyPayment status applied\",\"data\":{\"gateway\":\""
                            + (request.getGateway() != null ? request.getGateway() : "")
                            + "\",\"orderId\":\""
                            + request.getOrderId().trim()
                            + "\",\"responseStatus\":\""
                            + (response.getStatus() != null ? response.getStatus() : "")
                            + "\"},\"timestamp\":"
                            + System.currentTimeMillis()
                            + "}\n");
                } catch (Exception ignored) {}
                // #endregion
            } catch (NumberFormatException e) {
                // orderId invalid; verification response unchanged
            }
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Record payment failure (e.g. user closed Razorpay modal or gateway reported failure).
     * Idempotent: does not overwrite PAID or REFUNDED.
     */
    @PostMapping("/record-failed")
    public ResponseEntity<Map<String, Object>> recordPaymentFailed(@RequestBody Map<String, String> body) {
        String orderNumber = body != null ? body.get("orderNumber") : null;
        String gateway = body != null ? body.get("gateway") : null;
        if (orderNumber == null || orderNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderNumber is required"));
        }
        try {
            orderService.recordPaymentFailed(orderNumber.trim(), gateway != null ? gateway.trim() : null);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Order not found")) {
                return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }
}
