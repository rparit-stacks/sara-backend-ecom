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
            List<String> errors = new ArrayList<>();
            
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
                    
                    if (errorMessage != null) {
                        if (errorMessage.contains("File size exceeds")) {
                            source = "validation";
                        } else if (errorMessage.contains("Cloudinary error")) {
                            source = "cloudinary";
                        }
                    }
                    
                    Map<String, Object> errorDetail = new HashMap<>();
                    errorDetail.put("filename", file.getOriginalFilename());
                    errorDetail.put("error", errorMessage != null ? errorMessage : "Failed to upload file");
                    errorDetail.put("source", source);
                    errorDetail.put("fileSize", file.getSize());
                    errorDetail.put("maxSize", 10 * 1024 * 1024L);
                    
                    errors.add(errorDetail.toString());
                } catch (Exception e) {
                    System.err.println("[Product Media Upload] Error uploading " + file.getOriginalFilename() + ": " + e.getMessage());
                    errors.add("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("files", uploadedFiles);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
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
