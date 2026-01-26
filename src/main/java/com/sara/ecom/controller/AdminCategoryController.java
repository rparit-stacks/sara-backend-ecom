package com.sara.ecom.controller;

import com.sara.ecom.dto.CategoryDto;
import com.sara.ecom.dto.CategoryRequest;
import com.sara.ecom.service.CategoryService;
import com.sara.ecom.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;
    
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryDto category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryDto category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("[Category Image Upload] Received file: " + file.getOriginalFilename() + ", size: " + file.getSize());
            String imageUrl = cloudinaryService.uploadImage(file, "categories");
            System.out.println("[Category Image Upload] Uploaded to Cloudinary: " + imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (java.io.IOException e) {
            System.err.println("[Category Image Upload] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            
            String errorMessage = e.getMessage();
            String source = "system";
            
            if (errorMessage != null) {
                if (errorMessage.contains("File size exceeds")) {
                    source = "validation";
                } else if (errorMessage.contains("Cloudinary error")) {
                    source = "cloudinary";
                }
            }
            
            error.put("error", errorMessage != null ? errorMessage : "Failed to upload image");
            error.put("source", source);
            error.put("details", e.getCause() != null ? e.getCause().getMessage() : null);
            error.put("fileSize", file.getSize());
            error.put("maxSize", 10 * 1024 * 1024L); // 10MB
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            System.err.println("[Category Image Upload] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            error.put("source", "system");
            error.put("fileSize", file.getSize());
            error.put("maxSize", 10 * 1024 * 1024L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
