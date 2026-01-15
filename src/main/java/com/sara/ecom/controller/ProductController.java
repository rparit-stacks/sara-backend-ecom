package com.sara.ecom.controller;

import com.sara.ecom.dto.ProductDto;
import com.sara.ecom.dto.ProductRequest;
import com.sara.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // Public endpoints
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId) {
        List<ProductDto> products = productService.getAllProducts(status, type, categoryId);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    // Slug-based product route - must be after /products/{id} to avoid conflicts
    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ProductDto> getProductBySlug(@PathVariable String slug) {
        ProductDto product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(product);
    }
    
    @PostMapping("/products/batch")
    public ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        List<ProductDto> products = productService.getProductsByIds(ids);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Gets all active products for a category and all its child categories.
     * This is useful for category pages that should show products from subcategories too.
     */
    @GetMapping("/products/category/{categoryId}/with-children")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryWithChildren(@PathVariable Long categoryId) {
        List<ProductDto> products = productService.getProductsByCategoryWithChildren(categoryId);
        return ResponseEntity.ok(products);
    }
    
    // Admin endpoints
    @PostMapping("/admin/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        ProductDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/products/plain")
    public ResponseEntity<ProductDto> createPlainProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createPlainProduct(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/admin/products/designed")
    public ResponseEntity<ProductDto> createDesignedProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createDesignedProduct(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/admin/products/digital")
    public ResponseEntity<ProductDto> createDigitalProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createDigitalProduct(request);
        return ResponseEntity.ok(created);
    }
}
