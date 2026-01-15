package com.sara.ecom.controller;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.PlainProductRequest;
import com.sara.ecom.entity.Product;
import com.sara.ecom.service.PlainProductService;
import com.sara.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PlainProductController {
    
    @Autowired
    private PlainProductService plainProductService;
    
    @Autowired
    private ProductService productService;
    
    // Public endpoints
    @GetMapping("/plain-products")
    public ResponseEntity<List<PlainProductDto>> getAllPlainProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId) {
        List<PlainProductDto> products = plainProductService.getAllPlainProducts(status, categoryId);
        return ResponseEntity.ok(products);
    }
    
    // Get all active plain products (for admin product form)
    // This now fetches from products table where type=PLAIN, not from plain_products table
    @GetMapping("/plain-products/active")
    public ResponseEntity<List<PlainProductDto>> getActivePlainProducts() {
        // Fetch products where type=PLAIN and status=ACTIVE
        List<com.sara.ecom.dto.ProductDto> products = productService.getAllProducts("active", "PLAIN", null);
        
        // Convert ProductDto to PlainProductDto format for frontend compatibility
        List<PlainProductDto> plainProducts = products.stream()
            .map(p -> {
                PlainProductDto dto = new PlainProductDto();
                dto.setId(p.getId());
                dto.setName(p.getName());
                dto.setDescription(p.getDescription());
                dto.setImage(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null);
                dto.setPricePerMeter(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
                dto.setCategoryId(p.getCategoryId());
                dto.setStatus(p.getStatus());
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(plainProducts);
    }
    
    @GetMapping("/plain-products/{id}")
    public ResponseEntity<PlainProductDto> getPlainProductById(@PathVariable Long id) {
        PlainProductDto product = plainProductService.getPlainProductById(id);
        return ResponseEntity.ok(product);
    }
    
    @PostMapping("/plain-products/batch")
    public ResponseEntity<List<PlainProductDto>> getPlainProductsByIds(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        List<PlainProductDto> products = plainProductService.getPlainProductsByIds(ids);
        return ResponseEntity.ok(products);
    }
    
    // Admin endpoints
    @PostMapping("/admin/plain-products")
    public ResponseEntity<PlainProductDto> createPlainProduct(@RequestBody PlainProductRequest request) {
        PlainProductDto created = plainProductService.createPlainProduct(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/plain-products/{id}")
    public ResponseEntity<PlainProductDto> updatePlainProduct(
            @PathVariable Long id,
            @RequestBody PlainProductRequest request) {
        PlainProductDto updated = plainProductService.updatePlainProduct(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/plain-products/{id}")
    public ResponseEntity<Void> deletePlainProduct(@PathVariable Long id) {
        plainProductService.deletePlainProduct(id);
        return ResponseEntity.noContent().build();
    }
}
