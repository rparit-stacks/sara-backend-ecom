package com.sara.ecom.controller;

import com.sara.ecom.dto.DesignDto;
import com.sara.ecom.dto.DesignRequest;
import com.sara.ecom.service.DesignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DesignController {
    
    @Autowired
    private DesignService designService;
    
    // Public endpoints
    @GetMapping("/designs")
    public ResponseEntity<List<DesignDto>> getAllDesigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<DesignDto> designs = designService.getAllDesigns(status, category);
        return ResponseEntity.ok(designs);
    }
    
    @GetMapping("/designs/{id}")
    public ResponseEntity<DesignDto> getDesignById(@PathVariable Long id) {
        DesignDto design = designService.getDesignById(id);
        return ResponseEntity.ok(design);
    }
    
    @GetMapping("/designs/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = designService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    // Admin endpoints
    @PostMapping("/admin/designs")
    public ResponseEntity<DesignDto> createDesign(@RequestBody DesignRequest request) {
        DesignDto created = designService.createDesign(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/designs/{id}")
    public ResponseEntity<DesignDto> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignRequest request) {
        DesignDto updated = designService.updateDesign(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/designs/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}
