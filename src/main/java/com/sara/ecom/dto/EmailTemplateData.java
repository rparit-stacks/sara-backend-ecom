package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class EmailTemplateData {
    
    // Base data for all emails
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseEmailData {
        private String recipientName;
        private String recipientEmail;
        private String companyName;
        private String companyEmail;
        private String companyPhone;
        private String companyAddress;
    }
    
    // Order email data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderEmailData extends BaseEmailData {
        private String orderNumber;
        private String orderDate;
        private String orderStatus;
        private String paymentStatus;
        private BigDecimal subtotal;
        private BigDecimal gst;
        private BigDecimal shipping;
        private BigDecimal total;
        private BigDecimal couponDiscount;
        private String couponCode;
        private String shippingAddress;
        private String billingAddress;
        private List<OrderItemData> items;
        private String trackingNumber;
        private String trackingUrl;
        private String invoiceUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String productType;
    }
    
    // Cart email data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartEmailData extends BaseEmailData {
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private String productType;
        private String cartUrl;
    }
    
    // Wishlist email data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistEmailData extends BaseEmailData {
        private String productName;
        private String productImage;
        private BigDecimal price;
        private String productType;
        private String productUrl;
        private String wishlistUrl;
    }
    
    // Welcome email data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WelcomeEmailData extends BaseEmailData {
        private String loginUrl;
        private String shopUrl;
    }
    
    // Login notification data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginNotificationData extends BaseEmailData {
        private String loginTime;
        private String loginLocation;
        private String deviceInfo;
        private String changePasswordUrl;
    }
    
    // Design request email data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignRequestEmailData extends BaseEmailData {
        private Long requestId;
        private String requestDate;
        private String designType;
        private String description;
        private String referenceImage;
        private String status;
        private String adminNotes;
        private String requestUrl;
    }
    
    // Newsletter subscription data
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsletterSubscriptionData extends BaseEmailData {
        private String unsubscribeUrl;
    }
    
    // Contact form submission data (for admin)
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactFormData extends BaseEmailData {
        private String senderName;
        private String senderEmail;
        private String senderPhone;
        private String subject;
        private String message;
        private String submissionDate;
    }
}
