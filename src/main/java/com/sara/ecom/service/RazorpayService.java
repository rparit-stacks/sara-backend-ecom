package com.sara.ecom.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.sara.ecom.dto.PaymentRequest;
import com.sara.ecom.dto.PaymentResponse;
import com.sara.ecom.dto.PaymentVerificationRequest;
import com.sara.ecom.entity.PaymentConfig;
import com.sara.ecom.enums.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayService implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);
    
    @Autowired
    private PaymentConfigService paymentConfigService;
    
    private RazorpayClient getRazorpayClient() throws RazorpayException {
        String keyId = getKeyId();
        String keySecret = getKeySecret();
        
        if (keyId == null || keySecret == null || keyId.trim().isEmpty() || keySecret.trim().isEmpty()) {
            throw new RuntimeException("Razorpay is not configured. Please set API keys in admin panel.");
        }
        
        return new RazorpayClient(keyId, keySecret);
    }
    
    private String getKeyId() {
        try {
            PaymentConfig config = paymentConfigService.getConfigEntity();
            return config != null ? config.getRazorpayKeyId() : null;
        } catch (Exception e) {
            logger.error("Error getting Razorpay key ID", e);
            return null;
        }
    }
    
    private String getKeySecret() {
        try {
            PaymentConfig config = paymentConfigService.getConfigEntity();
            return config != null ? config.getRazorpayKeySecret() : null;
        } catch (Exception e) {
            logger.error("Error getting Razorpay key secret", e);
            return null;
        }
    }
    
    private boolean isRazorpayEnabled() {
        try {
            PaymentConfig config = paymentConfigService.getConfigEntity();
            if (config == null) return false;
            Boolean enabled = config.getRazorpayEnabled();
            String keyId = config.getRazorpayKeyId();
            String keySecret = config.getRazorpayKeySecret();
            return enabled != null && enabled && 
                   keyId != null && !keyId.trim().isEmpty() && 
                   keySecret != null && !keySecret.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.RAZORPAY;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            RazorpayClient razorpay = getRazorpayClient();
            
            // Convert amount to paise (smallest currency unit for INR)
            long amountInPaise = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getOrderNumber());
            orderRequest.put("notes", new JSONObject()
                .put("order_id", request.getOrderId())
                .put("order_number", request.getOrderNumber())
                .put("customer_email", request.getCustomerEmail()));
            
            Order order = razorpay.orders.create(orderRequest);
            
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("order_id", order.get("id"));
            orderData.put("amount", order.get("amount"));
            orderData.put("currency", order.get("currency"));
            orderData.put("key_id", getKeyId()); // Public key for frontend
            
            return PaymentResponse.builder()
                .paymentId(order.get("id").toString())
                .gateway("RAZORPAY")
                .status("created")
                .orderData(orderData)
                .message("Razorpay order created successfully")
                .build();
                
        } catch (RazorpayException e) {
            logger.error("Razorpay payment creation failed", e);
            throw new RuntimeException("Failed to create Razorpay payment: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        try {
            if (request.getVerificationData() == null) {
                return PaymentResponse.builder()
                    .paymentId(request.getPaymentId())
                    .gateway("RAZORPAY")
                    .status("FAILED")
                    .message("Missing verification data")
                    .build();
            }
            // Use verificationData for Razorpay IDs (signature = order_id|payment_id)
            String razorpayOrderId = (String) request.getVerificationData().get("razorpay_order_id");
            String paymentId = (String) request.getVerificationData().get("razorpay_payment_id");
            String signature = (String) request.getVerificationData().get("razorpay_signature");
            if (razorpayOrderId == null || paymentId == null || signature == null) {
                return PaymentResponse.builder()
                    .paymentId(paymentId != null ? paymentId : request.getPaymentId())
                    .gateway("RAZORPAY")
                    .status("FAILED")
                    .message("Missing razorpay_order_id, razorpay_payment_id or razorpay_signature")
                    .build();
            }
            // Razorpay signature verification using HMAC SHA256 (order_id|payment_id)
            String generatedSignature = generateRazorpaySignature(razorpayOrderId + "|" + paymentId, getKeySecret());
            boolean isValid = generatedSignature != null && generatedSignature.equals(signature);
            String status = isValid ? "SUCCESS" : "FAILED";
            return PaymentResponse.builder()
                .paymentId(paymentId)
                .gateway("RAZORPAY")
                .status(status)
                .message(isValid ? "Payment verified successfully" : "Payment verification failed")
                .build();
        } catch (Exception e) {
            logger.error("Razorpay payment verification failed", e);
            throw new RuntimeException("Failed to verify Razorpay payment: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isEnabled() {
        return isRazorpayEnabled();
    }
    
    @Override
    public List<String> getAvailableMethods(String country) {
        // Razorpay supports multiple payment methods
        return List.of("card", "upi", "netbanking", "wallet");
    }
    
    /**
     * Generate Razorpay signature using HMAC SHA256
     */
    private String generateRazorpaySignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error generating Razorpay signature", e);
            return null;
        }
    }
}
