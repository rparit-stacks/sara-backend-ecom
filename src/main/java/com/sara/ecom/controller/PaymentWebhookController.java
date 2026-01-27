package com.sara.ecom.controller;

import com.sara.ecom.entity.Order;
import com.sara.ecom.repository.OrderRepository;
import com.sara.ecom.service.OrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private com.sara.ecom.service.BusinessConfigService businessConfigService;

    @Autowired
    private OrderRepository orderRepository;
    
    @SuppressWarnings("unused")
    private String getStripeWebhookSecret() {
        try {
            businessConfigService.getConfigEntity();
            // TODO: Add webhook secret to BusinessConfig if needed
            // For now, return empty - webhook verification can be disabled for testing
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Stripe webhook handler
     */
    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            String webhookSecret = getStripeWebhookSecret();
            // Skip signature verification if secret is not configured (for testing)
            Event event;
            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                // Parse event without signature verification (not recommended for production)
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                event = mapper.readValue(payload, Event.class);
                logger.warn("Stripe webhook secret not configured - skipping signature verification");
            } else {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            }
            
            // Handle the event
            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

                if (paymentIntent != null) {
                    String orderId = paymentIntent.getMetadata().get("order_id");
                    if (orderId != null) {
                        // Update order payment status
                        Long id = Long.parseLong(orderId);
                        orderService.updatePaymentStatus(
                            id,
                            "PAID",
                            paymentIntent.getId()
                        );

                        // Also persist gateway currency and amount actually charged
                        try {
                            Order order = orderRepository.findById(id).orElse(null);
                            if (order != null) {
                                if (paymentIntent.getCurrency() != null) {
                                    order.setPaymentCurrency(paymentIntent.getCurrency().toUpperCase());
                                }
                                if (paymentIntent.getAmount() != null) {
                                    // Stripe amount is in smallest unit (e.g. cents)
                                    java.math.BigDecimal amt = java.math.BigDecimal
                                            .valueOf(paymentIntent.getAmount())
                                            .divide(java.math.BigDecimal.valueOf(100));
                                    order.setPaymentAmount(amt);
                                }
                                orderRepository.save(order);
                            }
                        } catch (Exception ex) {
                            logger.error("Failed to persist Stripe payment currency/amount for order {}", orderId, ex);
                        }
                        logger.info("Stripe payment succeeded for order: {}", orderId);
                    }
                }
            } else if ("payment_intent.payment_failed".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
                
                if (paymentIntent != null) {
                    String orderId = paymentIntent.getMetadata().get("order_id");
                    if (orderId != null) {
                        orderService.updatePaymentStatus(
                            Long.parseLong(orderId),
                            "FAILED",
                            paymentIntent.getId()
                        );
                        logger.info("Stripe payment failed for order: {}", orderId);
                    }
                }
            } else if ("payment_intent.canceled".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
                if (paymentIntent != null) {
                    String orderId = paymentIntent.getMetadata().get("order_id");
                    if (orderId != null) {
                        orderService.updatePaymentStatus(
                            Long.parseLong(orderId),
                            "FAILED",
                            paymentIntent.getId()
                        );
                        logger.info("Stripe payment canceled for order: {}", orderId);
                    }
                }
            }
            
            response.put("status", "success");
            return ResponseEntity.ok(response);
            
        } catch (SignatureVerificationException e) {
            logger.error("Stripe webhook signature verification failed", e);
            response.put("error", "Invalid signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error processing Stripe webhook", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Razorpay webhook handler
     */
    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            String event = (String) payload.get("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadData = (Map<String, Object>) payload.get("payload");
            
            if (payloadData == null) {
                response.put("error", "Invalid payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentEntity = (Map<String, Object>) payloadData.get("payment");
            @SuppressWarnings("unchecked")
            Map<String, Object> orderEntity = (Map<String, Object>) payloadData.get("order");
            
            if (paymentEntity == null || orderEntity == null) {
                response.put("error", "Missing payment or order data");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            String paymentId = (String) paymentEntity.get("id");
            @SuppressWarnings("unused")
            String razorpayOrderId = (String) orderEntity.get("id");
            
            // Get order ID from order notes or metadata
            String orderNumber = null;
            if (orderEntity.containsKey("notes")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> notes = (Map<String, Object>) orderEntity.get("notes");
                if (notes != null) {
                    orderNumber = (String) notes.get("order_number");
                }
            }
            
            if ("payment.captured".equals(event) || "payment.authorized".equals(event)) {
                // Find order by order number
                if (orderNumber != null) {
                    orderService.updatePaymentStatusByOrderNumber(orderNumber, "PAID", paymentId);

                    // Persist gateway currency and amount from Razorpay
                    try {
                        // paymentEntity contains amount (in smallest unit) and currency
                        Object currencyObj = paymentEntity.get("currency");
                        Object amountObj = paymentEntity.get("amount");
                        if (currencyObj != null && amountObj != null) {
                            String currency = String.valueOf(currencyObj).toUpperCase();
                            long amountMinor = Long.parseLong(String.valueOf(amountObj));
                            java.math.BigDecimal amountMajor = java.math.BigDecimal
                                    .valueOf(amountMinor)
                                    .divide(java.math.BigDecimal.valueOf(100));

                            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
                            if (order != null) {
                                order.setPaymentCurrency(currency);
                                order.setPaymentAmount(amountMajor);
                                orderRepository.save(order);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to persist Razorpay payment currency/amount for order {}", orderNumber, ex);
                    }
                    logger.info("Razorpay payment succeeded for order: {}", orderNumber);
                }
            } else if ("payment.failed".equals(event)) {
                if (orderNumber != null) {
                    orderService.updatePaymentStatusByOrderNumber(orderNumber, "FAILED", paymentId);
                    logger.info("Razorpay payment failed for order: {}", orderNumber);
                }
            }
            
            response.put("status", "success");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing Razorpay webhook", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
