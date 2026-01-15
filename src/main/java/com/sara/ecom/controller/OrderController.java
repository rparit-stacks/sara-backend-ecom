package com.sara.ecom.controller;

import com.sara.ecom.dto.CreateOrderRequest;
import com.sara.ecom.dto.OrderDto;
import com.sara.ecom.repository.UserRepository;
import com.sara.ecom.service.JwtService;
import com.sara.ecom.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
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
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateOrderRequest request) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(orderService.createOrder(userEmail, request));
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getUserOrders(@RequestHeader("Authorization") String authHeader) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(orderService.getUserOrders(userEmail));
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(orderService.getOrderById(id, userEmail));
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
