package com.sara.ecom.controller;

import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.CartDto;
import com.sara.ecom.exception.InvalidSessionException;
import com.sara.ecom.repository.UserRepository;
import com.sara.ecom.service.CartService;
import com.sara.ecom.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String couponCode) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(cartService.getCart(userEmail, state, couponCode));
    }
    
    @PostMapping
    public ResponseEntity<CartDto.CartItemDto> addToCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AddToCartRequest request) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(cartService.addToCart(userEmail, request));
    }
    
    @PutMapping("/{itemId}")
    public ResponseEntity<CartDto.CartItemDto> updateCartItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request) {
        String userEmail = getUserEmailFromToken(authHeader);
        Integer quantity = request.get("quantity");
        return ResponseEntity.ok(cartService.updateCartItem(userEmail, itemId, quantity));
    }

    /**
     * Full update of a CUSTOM cart item (design, fabric, variants, form, quantity, prices).
     * Only allowed when the existing item's productType is CUSTOM.
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto.CartItemDto> updateCartItemFull(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId,
            @RequestBody AddToCartRequest request) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(cartService.updateCartItemFull(userEmail, itemId, request));
    }
    
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId) {
        String userEmail = getUserEmailFromToken(authHeader);
        cartService.removeFromCart(userEmail, itemId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("Authorization") String authHeader) {
        String userEmail = getUserEmailFromToken(authHeader);
        cartService.clearCart(userEmail);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartItemCount(@RequestHeader("Authorization") String authHeader) {
        String userEmail = getUserEmailFromToken(authHeader);
        long count = cartService.getCartItemCount(userEmail);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    private String getUserEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractEmail(token);
        // Normalize email to lowercase to match how it's stored in database
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidSessionException("User not found"));
        return normalizedEmail;
    }
}
