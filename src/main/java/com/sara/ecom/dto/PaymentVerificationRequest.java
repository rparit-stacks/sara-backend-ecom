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
public class PaymentVerificationRequest {
    private String paymentId;
    private String orderId;
    private String gateway;
    private Map<String, Object> verificationData; // Gateway-specific verification data
}
