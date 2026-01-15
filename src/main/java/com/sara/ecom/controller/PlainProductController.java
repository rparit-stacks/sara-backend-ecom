package com.sara.ecom.controller;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.PlainProductRequest;
import com.sara.ecom.service.PlainProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlainProductController {
    
    @Autowired
    private PlainProductService plainProductService;
    
    // Public endpoints
    @GetMapping("/plain-products")
    public ResponseEntity<List<PlainProductDto>> getAllPlainProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId) {
        List<PlainProductDto> products = plainProductService.getAllPlainProducts(status, categoryId);
        return ResponseEntity.ok(products);
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
