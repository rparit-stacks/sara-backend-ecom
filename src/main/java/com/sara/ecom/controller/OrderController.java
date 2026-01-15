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
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
    
    @PutMapping("/admin/orders/{id}/payment")
    public ResponseEntity<OrderDto> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String paymentStatus = request.get("paymentStatus");
        String paymentId = request.get("paymentId");
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, paymentStatus, paymentId));
    }
    
    private String getUserEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractEmail(token);
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return email;
    }
}
