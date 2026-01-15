package com.sara.ecom.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    public CloudinaryService(
            @Value("${cloudinary.url:}") String cloudinaryUrl) {
        if (cloudinaryUrl != null && !cloudinaryUrl.isEmpty()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
        } else {
            // Fallback configuration
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dm7eyzc0c",
                "api_key", "988261371328627",
                "api_secret", "5JNztGOWVc5e7gsWespVYb8e2bg"
            ));
        }
    }
    
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", folder != null ? folder : "categories",
            "resource_type", "image",
            "overwrite", true,
            "width", 800,
            "height", 800,
            "crop", "limit"
        );
        
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }
    
    @SuppressWarnings("unchecked")
    public String uploadVideo(MultipartFile file, String folder) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", folder != null ? folder : "products/videos",
            "resource_type", "video",
            "overwrite", true
        );
        
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }
    
    @SuppressWarnings("unchecked")
    public String uploadMedia(MultipartFile file, String folder) throws IOException {
        // Detect if it's a video or image
        String contentType = file.getContentType();
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        
        if (isVideo) {
            return uploadVideo(file, folder);
        } else {
            return uploadImage(file, folder != null ? folder : "products/images");
        }
    }
    
    @SuppressWarnings("unchecked")
    public String uploadImageFromBase64(String base64Image, String folder) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", folder != null ? folder : "categories",
            "resource_type", "image",
            "overwrite", true,
            "width", 800,
            "height", 800,
            "crop", "limit"
        );
        
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(base64Image, params);
        return (String) uploadResult.get("secure_url");
    }
    
    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
            // Extract public_id from URL
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        }
    }
    
    private String extractPublicId(String url) {
        try {
            // Extract public_id from Cloudinary URL
            // Format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String afterUpload = parts[1];
                // Remove version if present
                if (afterUpload.contains("/v")) {
                    afterUpload = afterUpload.substring(afterUpload.indexOf("/v") + 2);
                    afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
                }
                // Remove file extension
                int lastDot = afterUpload.lastIndexOf(".");
                if (lastDot > 0) {
                    afterUpload = afterUpload.substring(0, lastDot);
                }
                return afterUpload;
            }
        } catch (Exception e) {
            // If extraction fails, return null
        }
        return null;
    }
}
