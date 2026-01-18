package com.sara.ecom.controller;

import com.sara.ecom.dto.CategoryDto;
import com.sara.ecom.service.CategoryService;
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
    
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(required = false) Boolean active,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        
        List<CategoryDto> categories;
        if (active != null && active) {
            // If user is authenticated, filter by email; otherwise show all active
            categories = userEmail != null 
                    ? categoryService.getActiveCategoriesForUser(userEmail)
                    : categoryService.getActiveCategories();
        } else {
            categories = categoryService.getAllCategories();
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
    public ResponseEntity<CategoryDto> getCategoryBySlugPath(HttpServletRequest request) {
        // Extract the path after /api/categories/
        String requestPath = request.getRequestURI();
        String slugPath = requestPath.replaceFirst("^/api/categories/?", "");
        
        // Remove leading/trailing slashes
        slugPath = slugPath.replaceAll("^/+|/+$", "");
        
        if (slugPath.isEmpty() || slugPath.equals("id") || slugPath.startsWith("id/") || slugPath.equals("leaf")) {
            return ResponseEntity.badRequest().build();
        }
        
        CategoryDto category = categoryService.getCategoryBySlugPath(slugPath);
        return ResponseEntity.ok(category);
    }
}
