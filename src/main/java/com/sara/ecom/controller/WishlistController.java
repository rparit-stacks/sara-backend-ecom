package com.sara.ecom.controller;

import com.sara.ecom.dto.WishlistDto;
import com.sara.ecom.repository.UserRepository;
import com.sara.ecom.service.JwtService;
import com.sara.ecom.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    
    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<WishlistDto>> getWishlist(@RequestHeader("Authorization") String authHeader) {
        String userEmail = getUserEmailFromToken(authHeader);
        return ResponseEntity.ok(wishlistService.getWishlist(userEmail));
    }
    
    @PostMapping
    public ResponseEntity<WishlistDto> addToWishlist(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        String userEmail = getUserEmailFromToken(authHeader);
        String productType = (String) request.get("productType");
        Long productId = Long.valueOf(request.get("productId").toString());
        return ResponseEntity.ok(wishlistService.addToWishlist(userEmail, productType, productId));
    }
    
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long itemId) {
        String userEmail = getUserEmailFromToken(authHeader);
        wishlistService.removeFromWishlist(userEmail, itemId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/product")
    public ResponseEntity<Void> removeFromWishlistByProduct(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String productType,
            @RequestParam Long productId) {
        String userEmail = getUserEmailFromToken(authHeader);
        wishlistService.removeFromWishlistByProduct(userEmail, productType, productId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String productType,
            @RequestParam Long productId) {
        String userEmail = getUserEmailFromToken(authHeader);
        boolean inWishlist = wishlistService.isInWishlist(userEmail, productType, productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
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
