package com.sara.ecom.controller;

import com.sara.ecom.dto.CategoryDto;
import com.sara.ecom.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(required = false) Boolean active) {
        List<CategoryDto> categories = active != null && active 
                ? categoryService.getActiveCategories() 
                : categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
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
