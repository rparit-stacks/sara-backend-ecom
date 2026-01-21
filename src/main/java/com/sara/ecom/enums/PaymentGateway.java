package com.sara.ecom.enums;

public enum PaymentGateway {
    STRIPE,
    RAZORPAY,
    COD, // Cash on Delivery
    PARTIAL_COD // Partial Cash on Delivery (advance online + remaining COD)
}
