package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String paymentId;
    private String gateway;
    private String status;
    private Map<String, Object> orderData; // Gateway-specific data (e.g., client_secret for Stripe, order_id for Razorpay)
    private String redirectUrl;
    private String message;
}
