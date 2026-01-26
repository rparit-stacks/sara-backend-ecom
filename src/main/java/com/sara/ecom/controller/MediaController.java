package com.sara.ecom.controller;

import com.sara.ecom.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class MediaController {
    
    private final CloudinaryService cloudinaryService;
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "banners") String folder) {
        try {
            System.out.println("[Media Upload] Received file: " + file.getOriginalFilename() + ", size: " + file.getSize());
            String imageUrl = cloudinaryService.uploadImage(file, folder);
            System.out.println("[Media Upload] Uploaded to Cloudinary: " + imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            System.err.println("[Media Upload] Error: " + e.getMessage());
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
            System.err.println("[Media Upload] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            error.put("source", "system");
            error.put("fileSize", file.getSize());
            error.put("maxSize", 10 * 1024 * 1024L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAllImages(
            @RequestParam(value = "folder", required = false) String folder) {
        try {
            List<Map<String, Object>> images = cloudinaryService.listAllImages(folder);
            Map<String, Object> response = new HashMap<>();
            response.put("images", images);
            response.put("count", images.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[Media List] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to list images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteImage(@RequestParam("url") String imageUrl) {
        try {
            cloudinaryService.deleteImage(imageUrl);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Image deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[Media Delete] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteImages(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> urls = (List<String>) request.get("urls");
            if (urls == null || urls.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No URLs provided");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> result = cloudinaryService.deleteImages(urls);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[Media Bulk Delete] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
