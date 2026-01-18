package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private BigDecimal amount;
    private String currency;
    private Long orderId;
    private String orderNumber;
    private String country;
    private String paymentGateway;
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String returnUrl;
    private String cancelUrl;


}
