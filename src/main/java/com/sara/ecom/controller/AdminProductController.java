package com.sara.ecom.controller;

import com.sara.ecom.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    
    private final CloudinaryService cloudinaryService;
    
    @PostMapping("/upload-media")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        try {
            System.out.println("[Product Media Upload] Received " + files.length + " files");
            
            List<Map<String, String>> uploadedFiles = new ArrayList<>();
            List<Map<String, Object>> errorDetails = new ArrayList<>();
            
            for (MultipartFile file : files) {
                try {
                    String contentType = file.getContentType();
                    boolean isVideo = contentType != null && contentType.startsWith("video/");
                    
                    System.out.println("[Product Media Upload] Uploading: " + file.getOriginalFilename() + 
                                     ", type: " + contentType + ", size: " + file.getSize());
                    
                    String url = cloudinaryService.uploadMedia(file, folder);
                    
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("url", url);
                    fileInfo.put("type", isVideo ? "video" : "image");
                    fileInfo.put("filename", file.getOriginalFilename());
                    uploadedFiles.add(fileInfo);
                    
                    System.out.println("[Product Media Upload] Success: " + url);
                } catch (java.io.IOException e) {
                    System.err.println("[Product Media Upload] Error uploading " + file.getOriginalFilename() + ": " + e.getMessage());
                    String errorMessage = e.getMessage();
                    String source = "system";
                    String userMessage = errorMessage != null ? errorMessage : "Failed to upload file";
                    
                    if (errorMessage != null) {
                        if (errorMessage.contains("File size exceeds") || errorMessage.contains("10MB") || errorMessage.contains("size")) {
                            source = "validation";
                            userMessage = "Upload failed: Cloudinary supports max 10MB. Please adjust file size or upgrade Cloudinary limits.";
                        } else if (errorMessage.contains("Cloudinary error") || errorMessage.contains("Cloudinary")) {
                            source = "cloudinary";
                            userMessage = "Upload failed: Cloudinary supports max 10MB. Please adjust file size or upgrade Cloudinary limits.";
                        }
                    }
                    
                    Map<String, Object> errorDetail = new HashMap<>();
                    errorDetail.put("filename", file.getOriginalFilename());
                    errorDetail.put("error", errorMessage != null ? errorMessage : "Failed to upload file");
                    errorDetail.put("source", source);
                    errorDetail.put("userMessage", userMessage);
                    errorDetail.put("fileSize", file.getSize());
                    errorDetail.put("maxSize", 10 * 1024 * 1024L);
                    
                    errorDetails.add(errorDetail);
                } catch (Exception e) {
                    System.err.println("[Product Media Upload] Error uploading " + file.getOriginalFilename() + ": " + e.getMessage());
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("filename", file.getOriginalFilename());
                    fallback.put("error", e.getMessage() != null ? e.getMessage() : "Failed to upload file");
                    fallback.put("source", "system");
                    fallback.put("userMessage", "Upload failed. Please check your connection and try again.");
                    errorDetails.add(fallback);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("files", uploadedFiles);
            if (!errorDetails.isEmpty()) {
                response.put("errors", errorDetails);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[Product Media Upload] Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload media: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
