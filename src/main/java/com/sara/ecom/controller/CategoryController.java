package com.sara.ecom.controller;

import com.sara.ecom.dto.CategoryDto;
import com.sara.ecom.service.CategoryService;
import com.sara.ecom.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    private final JwtService jwtService;
    
    /**
     * Helper method to check if request is from admin
     */
    private boolean isAdminRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtService.isAdminToken(token);
            } catch (Exception e) {
                return false;
            }
        }
        // Also check if path is admin endpoint
        return request.getRequestURI().startsWith("/api/admin/");
    }
    
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String userEmail,
            Authentication authentication,
            HttpServletRequest request) {
        // Use provided userEmail or get from authentication
        String email = userEmail != null ? userEmail : (authentication != null ? authentication.getName() : null);
        boolean isAdmin = isAdminRequest(request);
        
        List<CategoryDto> categories;
        if (active != null && active) {
            categories = categoryService.getActiveCategories(email, isAdmin);
        } else {
            categories = categoryService.getAllCategories(email, isAdmin);
        }
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Gets categories for the authenticated user (for dashboard).
     * Only shows categories assigned to the user via email.
     */
    @GetMapping("/my-categories")
    public ResponseEntity<List<CategoryDto>> getMyCategories(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of()); // Return empty list if not authenticated
        }
        String userEmail = authentication.getName();
        List<CategoryDto> categories = categoryService.getActiveCategoriesForUser(userEmail);
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Gets dashboard notification for restricted categories.
     * Returns notification message if user has access to restricted categories.
     */
    @GetMapping("/dashboard-notification")
    public ResponseEntity<Map<String, Object>> getDashboardNotification(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("hasRestrictedCategories", false));
        }
        String userEmail = authentication.getName();
        List<CategoryDto> restrictedCategories = categoryService.getActiveCategoriesForUser(userEmail);
        
        // Check if user has any restricted categories (categories with allowedEmails)
        boolean hasRestrictedCategories = restrictedCategories.stream()
                .anyMatch(cat -> cat.getAllowedEmails() != null && !cat.getAllowedEmails().trim().isEmpty());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasRestrictedCategories", hasRestrictedCategories);
        if (hasRestrictedCategories) {
            response.put("message", "The store has loaded special products for you.");
            response.put("categories", restrictedCategories.stream()
                    .filter(cat -> cat.getAllowedEmails() != null && !cat.getAllowedEmails().trim().isEmpty())
                    .map(cat -> Map.of("id", cat.getId(), "name", cat.getName() != null ? cat.getName() : ""))
                    .collect(java.util.stream.Collectors.toList()));
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/id/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        CategoryDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }
    
    @GetMapping("/leaf")
    public ResponseEntity<List<CategoryDto>> getLeafCategories() {
        List<CategoryDto> categories = categoryService.getLeafCategories();
        return ResponseEntity.ok(categories);
    }
    
    // This must be last to catch all slug paths like "men/shirts/formal-shirts"
    // Using a catch-all pattern that works with Spring Boot
    @GetMapping(value = "/**")
    public ResponseEntity<CategoryDto> getCategoryBySlugPath(
            HttpServletRequest request,
            @RequestParam(required = false) String userEmail,
            Authentication authentication) {
        // Extract the path after /api/categories/
        String requestPath = request.getRequestURI();
        String slugPath = requestPath.replaceFirst("^/api/categories/?", "");
        
        // Remove leading/trailing slashes
        slugPath = slugPath.replaceAll("^/+|/+$", "");
        
        if (slugPath.isEmpty() || slugPath.equals("id") || slugPath.startsWith("id/") || slugPath.equals("leaf")) {
            return ResponseEntity.badRequest().build();
        }
        
        // Use provided userEmail or get from authentication
        String email = userEmail != null ? userEmail : (authentication != null ? authentication.getName() : null);
        boolean isAdmin = isAdminRequest(request);
        
        CategoryDto category = categoryService.getCategoryBySlugPath(slugPath, email, isAdmin);
        return ResponseEntity.ok(category);
    }
}
