package com.sara.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    
    private Long parentId;
    
    private String status; // ACTIVE or INACTIVE
    
    private String image;
    
    private String description;
    
    private Integer displayOrder;
    
    private Boolean isFabric;
    
    // Comma-separated list of emails for category access restriction
    private String allowedEmails;
}
