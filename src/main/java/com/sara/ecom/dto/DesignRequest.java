package com.sara.ecom.dto;

import java.util.List;

public class DesignRequest {
    private String name;
    private String category;
    private String image;
    private String description;
    private String status;
    private List<Long> assignedFabricIds;
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<Long> getAssignedFabricIds() {
        return assignedFabricIds;
    }
    
    public void setAssignedFabricIds(List<Long> assignedFabricIds) {
        this.assignedFabricIds = assignedFabricIds;
    }
}
