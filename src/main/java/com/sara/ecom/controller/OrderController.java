package com.sara.ecom.controller;

import com.sara.ecom.dto.CreateOrderRequest;
import com.sara.ecom.dto.OrderDto;
import com.sara.ecom.repository.UserRepository;
import com.sara.ecom.service.JwtService;
import com.sara.ecom.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserRepository userRepository;
    
    // User endpoints
    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreateOrderRequest request) {
        String userEmail = null;
        if (authHeader != null && !authHeader.isEmpty()) {
            try {
                userEmail = getUserEmailFromToken(authHeader);
            } catch (Exception e) {
                // Guest checkout - userEmail will be null
            }
        }
        return ResponseEntity.ok(orderService.createOrder(userEmail, request));
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getUserOrders(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(orderService.getUserOrders(userEmail));
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        // If authenticated, verify user owns the order
        if (authHeader != null && !authHeader.isEmpty()) {
            try {
                String userEmail = getUserEmailFromToken(authHeader);
                return ResponseEntity.ok(orderService.getOrderById(id, userEmail));
            } catch (Exception e) {
                // If token is invalid, fall through to public access
            }
        }
        // Public access for order confirmation page (no auth required)
        return ResponseEntity.ok(orderService.getOrderByIdPublic(id));
    }
    
    // Admin endpoints
    @GetMapping("/admin/orders")
    public ResponseEntity<List<OrderDto>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }
    
    @GetMapping("/admin/orders/{id}")
    public ResponseEntity<OrderDto> getOrderByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderByIdAdmin(id));
    }
    
    @PutMapping("/admin/orders/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        String status = (String) request.get("status");
        String customStatus = (String) request.get("customStatus");
        String customMessage = (String) request.get("customMessage");
        Boolean skipWhatsApp = false;
        if (request.get("skipWhatsApp") != null) {
            if (request.get("skipWhatsApp") instanceof Boolean) {
                skipWhatsApp = (Boolean) request.get("skipWhatsApp");
            } else if (request.get("skipWhatsApp") instanceof String) {
                skipWhatsApp = Boolean.parseBoolean((String) request.get("skipWhatsApp"));
            }
        }
        
        // If customStatus is provided, use updateCustomStatus
        if (customStatus != null && !customStatus.trim().isEmpty()) {
            return ResponseEntity.ok(orderService.updateCustomStatus(id, customStatus, customMessage, skipWhatsApp, changedBy));
        } else {
            // Use standard status update
            return ResponseEntity.ok(orderService.updateOrderStatus(id, status, customStatus, customMessage, skipWhatsApp, changedBy));
        }
    }
    
    @PutMapping("/admin/orders/{id}/payment")
    public ResponseEntity<OrderDto> updatePaymentStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        String paymentStatus = (String) request.get("paymentStatus");
        String paymentId = (String) request.get("paymentId");
        Object paymentAmountObj = request.get("paymentAmount");
        java.math.BigDecimal paymentAmount = null;
        if (paymentAmountObj != null) {
            if (paymentAmountObj instanceof Number) {
                paymentAmount = java.math.BigDecimal.valueOf(((Number) paymentAmountObj).doubleValue());
            } else if (paymentAmountObj instanceof String) {
                try {
                    paymentAmount = new java.math.BigDecimal((String) paymentAmountObj);
                } catch (NumberFormatException e) {
                    // Ignore invalid number
                }
            }
        }
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, paymentStatus, paymentId, paymentAmount, changedBy));
    }
    
    @PutMapping("/admin/orders/{id}/notes")
    public ResponseEntity<OrderDto> updateOrderNotes(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        String notes = request.get("notes");
        return ResponseEntity.ok(orderService.updateOrderNotes(id, notes, changedBy));
    }
    
    @PutMapping("/admin/orders/{id}/cancellation")
    public ResponseEntity<OrderDto> updateCancellationInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        String cancellationReason = request.get("cancellationReason");
        String cancelledBy = request.get("cancelledBy");
        return ResponseEntity.ok(orderService.updateCancellationInfo(id, cancellationReason, cancelledBy, changedBy));
    }
    
    @PutMapping("/admin/orders/{id}/refund")
    public ResponseEntity<OrderDto> updateRefundInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        java.math.BigDecimal refundAmount = parseBigDecimal(request.get("refundAmount"));
        java.time.LocalDateTime refundDate = null;
        if (request.get("refundDate") != null) {
            Object refundDateObj = request.get("refundDate");
            if (refundDateObj instanceof String) {
                try {
                    // Try parsing as ISO date string (YYYY-MM-DD)
                    String dateStr = (String) refundDateObj;
                    if (dateStr.length() == 10) {
                        refundDate = java.time.LocalDate.parse(dateStr).atStartOfDay();
                    } else {
                        refundDate = java.time.LocalDateTime.parse(dateStr);
                    }
                } catch (Exception e) {
                    // If parsing fails, use current time
                    refundDate = java.time.LocalDateTime.now();
                }
            }
        } else if (refundAmount != null && refundAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            refundDate = java.time.LocalDateTime.now();
        }
        String refundTransactionId = (String) request.get("refundTransactionId");
        String refundReason = (String) request.get("refundReason");
        return ResponseEntity.ok(orderService.updateRefundInfo(id, refundAmount, refundDate, refundTransactionId, refundReason, changedBy));
    }
    
    @GetMapping("/admin/orders/{id}/payment-history")
    public ResponseEntity<List<com.sara.ecom.dto.OrderPaymentHistoryDto>> getPaymentHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getPaymentHistory(id));
    }
    
    @GetMapping("/admin/orders/{id}/audit-log")
    public ResponseEntity<List<com.sara.ecom.dto.OrderAuditLogDto>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getAuditLog(id));
    }
    
    @PostMapping("/admin/orders/{id}/payment-history")
    public ResponseEntity<com.sara.ecom.dto.OrderPaymentHistoryDto> addPaymentHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        String paymentType = (String) request.get("paymentType");
        java.math.BigDecimal amount = parseBigDecimal(request.get("amount"));
        String currency = (String) request.getOrDefault("currency", "INR");
        String transactionId = (String) request.get("transactionId");
        String paymentMethod = (String) request.get("paymentMethod");
        java.time.LocalDateTime paidAt = null;
        if (request.get("paidAt") != null) {
            Object paidAtObj = request.get("paidAt");
            if (paidAtObj instanceof String) {
                try {
                    String dateStr = (String) paidAtObj;
                    if (dateStr.length() == 10) {
                        paidAt = java.time.LocalDate.parse(dateStr).atStartOfDay();
                    } else {
                        paidAt = java.time.LocalDateTime.parse(dateStr);
                    }
                } catch (Exception e) {
                    paidAt = java.time.LocalDateTime.now();
                }
            }
        }
        String notes = (String) request.get("notes");
        com.sara.ecom.dto.OrderPaymentHistoryDto history = orderService.addPaymentHistory(id, paymentType, amount, currency, 
                transactionId, paymentMethod, paidAt, notes);
        // Log audit entry
        orderService.logAuditEntry(id, changedBy, "PAYMENT_HISTORY_ADD", "paymentHistory", null, 
                paymentType + " - " + amount.toString(), notes != null ? notes : "Payment history entry added");
        return ResponseEntity.ok(history);
    }
    
    @PutMapping("/admin/orders/{id}/items/{itemId}")
    public ResponseEntity<OrderDto> updateOrderItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        Integer quantity = request.get("quantity") != null ? ((Number) request.get("quantity")).intValue() : null;
        java.math.BigDecimal price = null;
        if (request.get("price") != null) {
            Object priceObj = request.get("price");
            if (priceObj instanceof Number) {
                price = java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                try {
                    price = new java.math.BigDecimal((String) priceObj);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        String name = (String) request.get("name");
        return ResponseEntity.ok(orderService.updateOrderItem(id, itemId, quantity, price, name, changedBy));
    }
    
    @PutMapping("/admin/orders/{id}/pricing")
    public ResponseEntity<OrderDto> updateOrderPricing(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        java.math.BigDecimal subtotal = parseBigDecimal(request.get("subtotal"));
        java.math.BigDecimal gst = parseBigDecimal(request.get("gst"));
        java.math.BigDecimal shipping = parseBigDecimal(request.get("shipping"));
        java.math.BigDecimal total = parseBigDecimal(request.get("total"));
        return ResponseEntity.ok(orderService.updateOrderPricing(id, subtotal, gst, shipping, total, changedBy));
    }
    
    @PostMapping("/admin/orders/{id}/recalculate")
    public ResponseEntity<OrderDto> recalculateOrderTotals(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        return ResponseEntity.ok(orderService.recalculateOrderTotals(id, changedBy));
    }
    
    private java.math.BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                return new java.math.BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @PostMapping("/admin/orders/{id}/retry-swipe-invoice")
    public ResponseEntity<OrderDto> retrySwipeInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.retrySwipeInvoice(id));
    }
    
    @GetMapping("/admin/orders/{id}/check-swipe-invoice")
    public ResponseEntity<Map<String, Object>> checkSwipeInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.checkSwipeInvoiceStatus(id));
    }

    @PutMapping("/admin/orders/{id}/address")
    public ResponseEntity<OrderDto> updateOrderShippingAddressAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        Object shippingAddressObj = request.get("shippingAddress");
        if (!(shippingAddressObj instanceof Map)) {
            throw new RuntimeException("shippingAddress is required");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> shippingAddress = (Map<String, Object>) shippingAddressObj;
        return ResponseEntity.ok(orderService.updateOrderShippingAddressAdmin(id, shippingAddress, changedBy));
    }
    
    @PutMapping("/admin/orders/{id}/billing-address")
    public ResponseEntity<OrderDto> updateOrderBillingAddressAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String changedBy = getAdminEmailFromHeader(authHeader);
        Object billingAddressObj = request.get("billingAddress");
        if (!(billingAddressObj instanceof Map)) {
            throw new RuntimeException("billingAddress is required");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> billingAddress = (Map<String, Object>) billingAddressObj;
        return ResponseEntity.ok(orderService.updateOrderBillingAddressAdmin(id, billingAddress, changedBy));
    }
    
    private String getAdminEmailFromHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return "system";
        }
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            return jwtService.extractEmail(token);
        } catch (Exception e) {
            return "system";
        }
    }
    
    private String getUserEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractEmail(token);
        // Normalize email to lowercase to match how it's stored in database
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return normalizedEmail;
    }
}
