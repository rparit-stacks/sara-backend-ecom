package com.sara.ecom.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.ProductRequest;
import com.sara.ecom.entity.CustomProduct;
import com.sara.ecom.service.CustomProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/custom-products")
@RequiredArgsConstructor
public class CustomProductController {
    
    private final CustomProductService customProductService;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a custom product from Make Your Own section (unsaved by default).
     * This product will never appear in public listings.
     * Anyone can create custom products - if logged in, uses their email; if not, uses temporary identifier.
     */
    @PostMapping
    public ResponseEntity<CustomProduct> createCustomProduct(
            @RequestBody ProductRequest request,
            Authentication authentication) {
        // Get user email from authentication if available, otherwise use temporary identifier
        String userEmail;
        if (authentication != null && authentication.getName() != null) {
            userEmail = authentication.getName();
        } else {
            // For guest users, use a temporary identifier
            // If email provided in request, use it; otherwise generate temp ID
            if (request.getUserEmail() != null && !request.getUserEmail().trim().isEmpty()) {
                userEmail = request.getUserEmail().trim().toLowerCase();
            } else {
                // Generate temporary identifier for guest users
                // Format: guest_timestamp_random
                userEmail = "guest_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
            }
        }
        
        // Convert mockup URLs list to JSON string
        String mockupUrlsJson = null;
        if (request.getMockupUrls() != null && !request.getMockupUrls().isEmpty()) {
            try {
                mockupUrlsJson = objectMapper.writeValueAsString(request.getMockupUrls());
            } catch (JsonProcessingException e) {
                // If JSON serialization fails, store as empty array
                mockupUrlsJson = "[]";
            }
        }
        
        // Convert ProductRequest to CustomProduct
        CustomProduct customProduct = CustomProduct.builder()
                .userEmail(userEmail)
                .isSaved(false) // Default to unsaved - will be auto-deleted if not saved
                .productName(request.getName() != null ? request.getName() : "Custom Design")
                .designUrl(request.getImages() != null && !request.getImages().isEmpty() 
                        ? request.getImages().get(0) : null)
                .mockupUrls(mockupUrlsJson)
                .designPrice(request.getDesignPrice() != null 
                        ? BigDecimal.valueOf(request.getDesignPrice().doubleValue()) : null)
                .quantity(1)
                .build();
        
        CustomProduct saved = customProductService.saveCustomProduct(customProduct);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Creates a custom product from CustomProduct object (for direct creation).
     */
    @PostMapping("/direct")
    public ResponseEntity<CustomProduct> createCustomProductDirect(
            @RequestBody CustomProduct customProduct,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String userEmail = authentication.getName();
        customProduct.setUserEmail(userEmail);
        customProduct.setIsSaved(false); // Default to unsaved
        CustomProduct saved = customProductService.saveCustomProduct(customProduct);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Deletes an unsaved custom product when user leaves/cancels.
     * This is called when user navigates away without saving.
     * Works for both authenticated and guest users.
     */
    @DeleteMapping("/unsaved/{id}")
    public ResponseEntity<Void> deleteUnsavedCustomProduct(
            @PathVariable Long id,
            @RequestParam(required = false) String userEmail,
            Authentication authentication) {
        // Get user email from authentication or request parameter
        String email;
        if (authentication != null && authentication.getName() != null) {
            email = authentication.getName();
        } else if (userEmail != null && !userEmail.trim().isEmpty()) {
            email = userEmail.trim().toLowerCase();
        } else {
            return ResponseEntity.status(400).build(); // Bad request - need userEmail
        }
        
        customProductService.deleteUnsavedCustomProduct(id, email);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Gets all saved custom products for the authenticated user.
     */
    @GetMapping("/saved")
    public ResponseEntity<List<CustomProduct>> getSavedCustomProducts(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of());
        }
        String userEmail = authentication.getName();
        List<CustomProduct> products = customProductService.getSavedCustomProducts(userEmail);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Gets all custom products for the authenticated user (saved and unsaved).
     */
    @GetMapping
    public ResponseEntity<List<CustomProduct>> getAllCustomProducts(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of());
        }
        String userEmail = authentication.getName();
        List<CustomProduct> products = customProductService.getAllCustomProducts(userEmail);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Marks a custom product as saved.
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<CustomProduct> saveCustomProduct(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String userEmail = authentication.getName();
        CustomProduct saved = customProductService.saveCustomProduct(id, userEmail);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Gets a custom product by ID (only if user owns it).
     * Works for both authenticated and guest users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomProduct> getCustomProductById(
            @PathVariable Long id,
            @RequestParam(required = false) String userEmail,
            Authentication authentication) {
        // Get user email from authentication or request parameter
        String email;
        if (authentication != null && authentication.getName() != null) {
            email = authentication.getName();
        } else if (userEmail != null && !userEmail.trim().isEmpty()) {
            email = userEmail.trim().toLowerCase();
        } else {
            return ResponseEntity.status(400).build(); // Bad request - need userEmail
        }
        
        CustomProduct product = customProductService.getCustomProductById(id, email);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Deletes a custom product.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomProduct(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String userEmail = authentication.getName();
        customProductService.deleteCustomProduct(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
