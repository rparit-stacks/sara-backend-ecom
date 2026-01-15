package com.sara.ecom.dto;

import java.util.List;

public class DesignDto {
    private Long id;
    private String name;
    private String category;
    private String image;
    private String description;
    private String status;
    private int fabricCount;
    private List<Long> assignedFabricIds;
    private List<FabricSummary> assignedFabrics;
    
    public static class FabricSummary {
        private Long id;
        private String name;
        private String image;
        private String status;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getImage() {
            return image;
        }
        
        public void setImage(String image) {
            this.image = image;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public int getFabricCount() {
        return fabricCount;
    }
    
    public void setFabricCount(int fabricCount) {
        this.fabricCount = fabricCount;
    }
    
    public List<Long> getAssignedFabricIds() {
        return assignedFabricIds;
    }
    
    public void setAssignedFabricIds(List<Long> assignedFabricIds) {
        this.assignedFabricIds = assignedFabricIds;
    }
    
    public List<FabricSummary> getAssignedFabrics() {
        return assignedFabrics;
    }
    
    public void setAssignedFabrics(List<FabricSummary> assignedFabrics) {
        this.assignedFabrics = assignedFabrics;
    }
}
