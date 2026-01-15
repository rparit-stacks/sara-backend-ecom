package com.sara.ecom.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CMSDto {
    
    // Homepage response
    public static class HomepageResponse {
        private List<Long> bestSellerIds;
        private List<Long> newArrivalIds;
        private List<TestimonialDto> testimonials;
        private List<OfferDto> offers;
        private List<String> instagramPosts;
        private List<BannerDto> banners;
        private Map<String, String> landingContent;
        private Map<String, String> contactInfo;
        
        // Getters and Setters
        public List<Long> getBestSellerIds() { return bestSellerIds; }
        public void setBestSellerIds(List<Long> bestSellerIds) { this.bestSellerIds = bestSellerIds; }
        public List<Long> getNewArrivalIds() { return newArrivalIds; }
        public void setNewArrivalIds(List<Long> newArrivalIds) { this.newArrivalIds = newArrivalIds; }
        public List<TestimonialDto> getTestimonials() { return testimonials; }
        public void setTestimonials(List<TestimonialDto> testimonials) { this.testimonials = testimonials; }
        public List<OfferDto> getOffers() { return offers; }
        public void setOffers(List<OfferDto> offers) { this.offers = offers; }
        public List<String> getInstagramPosts() { return instagramPosts; }
        public void setInstagramPosts(List<String> instagramPosts) { this.instagramPosts = instagramPosts; }
        public List<BannerDto> getBanners() { return banners; }
        public void setBanners(List<BannerDto> banners) { this.banners = banners; }
        public Map<String, String> getLandingContent() { return landingContent; }
        public void setLandingContent(Map<String, String> landingContent) { this.landingContent = landingContent; }
        public Map<String, String> getContactInfo() { return contactInfo; }
        public void setContactInfo(Map<String, String> contactInfo) { this.contactInfo = contactInfo; }
    }
    
    public static class TestimonialDto {
        private Long id;
        private String name;
        private String text;
        private Integer rating;
        private String location;
        private Boolean isActive;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class TestimonialRequest {
        private String name;
        private String text;
        private Integer rating;
        private String location;
        private Boolean isActive;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class TestimonialSubmitRequest {
        private String name;
        private String text;
        private Integer rating;
        private String location;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }
    
    public static class TestimonialLinkDto {
        private Long id;
        private String linkId;
        private String fullLink;
        private Boolean isUsed;
        private LocalDateTime createdAt;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLinkId() { return linkId; }
        public void setLinkId(String linkId) { this.linkId = linkId; }
        public String getFullLink() { return fullLink; }
        public void setFullLink(String fullLink) { this.fullLink = fullLink; }
        public Boolean getIsUsed() { return isUsed; }
        public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
    
    public static class OfferDto {
        private Long id;
        private String title;
        private String description;
        private Boolean isActive;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class OfferRequest {
        private String title;
        private String description;
        private Boolean isActive;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class BannerDto {
        private Long id;
        private String image;
        private String title;
        private String subtitle;
        private String link;
        private String buttonText;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean isActive;
        private Integer displayOrder;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getButtonText() { return buttonText; }
        public void setButtonText(String buttonText) { this.buttonText = buttonText; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
    
    public static class BannerRequest {
        private String image;
        private String title;
        private String subtitle;
        private String link;
        private String buttonText;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean isActive;
        private Integer displayOrder;
        
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getButtonText() { return buttonText; }
        public void setButtonText(String buttonText) { this.buttonText = buttonText; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
    
    public static class HomepageSectionRequest {
        private List<Long> productIds;
        
        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    }
    
    public static class InstagramPostRequest {
        private List<String> imageUrls;
        
        public List<String> getImageUrls() { return imageUrls; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    }
    
    public static class ContentRequest {
        private Map<String, String> content;
        
        public Map<String, String> getContent() { return content; }
        public void setContent(Map<String, String> content) { this.content = content; }
    }
}
