package com.sara.ecom.dto;

import java.time.LocalDateTime;

public class BlogDto {
    private Long id;
    private String title;
    private String excerpt;
    private String content;
    private String image;
    private String author;
    private String category;
    private String status;
    private Long views;
    private LocalDateTime publishedAt;
    private String readTime;
    private Boolean isHomepageFeatured;
    private Integer homepagePosition;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getExcerpt() {
        return excerpt;
    }
    
    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getViews() {
        return views;
    }
    
    public void setViews(Long views) {
        this.views = views;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public String getReadTime() {
        return readTime;
    }
    
    public void setReadTime(String readTime) {
        this.readTime = readTime;
    }
    
    public Boolean getIsHomepageFeatured() {
        return isHomepageFeatured;
    }
    
    public void setIsHomepageFeatured(Boolean isHomepageFeatured) {
        this.isHomepageFeatured = isHomepageFeatured;
    }
    
    public Integer getHomepagePosition() {
        return homepagePosition;
    }
    
    public void setHomepagePosition(Integer homepagePosition) {
        this.homepagePosition = homepagePosition;
    }
}
