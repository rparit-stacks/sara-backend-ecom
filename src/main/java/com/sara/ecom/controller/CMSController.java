package com.sara.ecom.controller;

import com.sara.ecom.dto.CMSDto;
import com.sara.ecom.service.CMSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CMSController {
    
    @Autowired
    private CMSService cmsService;
    
    // ===== Public Endpoints =====
    
    @GetMapping("/cms/homepage")
    public ResponseEntity<CMSDto.HomepageResponse> getHomepage() {
        return ResponseEntity.ok(cmsService.getHomepageData());
    }
    
    @GetMapping("/cms/best-sellers")
    public ResponseEntity<List<Long>> getBestSellers() {
        return ResponseEntity.ok(cmsService.getBestSellerIds());
    }
    
    @GetMapping("/cms/new-arrivals")
    public ResponseEntity<List<Long>> getNewArrivals() {
        return ResponseEntity.ok(cmsService.getNewArrivalIds());
    }
    
    @GetMapping("/cms/testimonials")
    public ResponseEntity<List<CMSDto.TestimonialDto>> getTestimonials() {
        return ResponseEntity.ok(cmsService.getActiveTestimonials());
    }
    
    @GetMapping("/cms/offers")
    public ResponseEntity<List<CMSDto.OfferDto>> getOffers() {
        return ResponseEntity.ok(cmsService.getActiveOffers());
    }
    
    @GetMapping("/cms/instagram")
    public ResponseEntity<List<String>> getInstagram() {
        return ResponseEntity.ok(cmsService.getInstagramPosts());
    }
    
    @GetMapping("/cms/banners")
    public ResponseEntity<List<CMSDto.BannerDto>> getBanners() {
        return ResponseEntity.ok(cmsService.getActiveBanners());
    }
    
    @GetMapping("/cms/landing")
    public ResponseEntity<Map<String, String>> getLandingContent() {
        return ResponseEntity.ok(cmsService.getLandingContent());
    }
    
    @GetMapping("/cms/contact")
    public ResponseEntity<Map<String, String>> getContactInfo() {
        return ResponseEntity.ok(cmsService.getContactInfo());
    }
    
    // ===== Testimonial Submission (via link) =====
    
    @PostMapping("/testimonials/submit/{linkId}")
    public ResponseEntity<CMSDto.TestimonialDto> submitTestimonial(
            @PathVariable String linkId,
            @RequestBody CMSDto.TestimonialSubmitRequest request) {
        return ResponseEntity.ok(cmsService.submitTestimonial(linkId, request));
    }
    
    // Email Subscription
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribeEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        try {
            cmsService.subscribeEmail(email);
            return ResponseEntity.ok(Map.of("message", "Successfully subscribed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Fetch Instagram thumbnail
    @PostMapping("/instagram/thumbnail")
    public ResponseEntity<Map<String, String>> getInstagramThumbnail(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }
        try {
            String thumbnailUrl = cmsService.getInstagramThumbnail(url);
            return ResponseEntity.ok(Map.of("thumbnailUrl", thumbnailUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // ===== Admin Endpoints =====
    
    // Best Sellers
    @PutMapping("/admin/cms/best-sellers")
    public ResponseEntity<Void> setBestSellers(@RequestBody CMSDto.HomepageSectionRequest request) {
        cmsService.setBestSellers(request.getProductIds());
        return ResponseEntity.ok().build();
    }
    
    // New Arrivals
    @PutMapping("/admin/cms/new-arrivals")
    public ResponseEntity<Void> setNewArrivals(@RequestBody CMSDto.HomepageSectionRequest request) {
        cmsService.setNewArrivals(request.getProductIds());
        return ResponseEntity.ok().build();
    }
    
    // Testimonials
    @GetMapping("/admin/cms/testimonials")
    public ResponseEntity<List<CMSDto.TestimonialDto>> getAllTestimonials() {
        return ResponseEntity.ok(cmsService.getAllTestimonials());
    }
    
    @PostMapping("/admin/cms/testimonials")
    public ResponseEntity<CMSDto.TestimonialDto> createTestimonial(@RequestBody CMSDto.TestimonialRequest request) {
        return ResponseEntity.ok(cmsService.createTestimonial(request));
    }
    
    @PutMapping("/admin/cms/testimonials/{id}")
    public ResponseEntity<CMSDto.TestimonialDto> updateTestimonial(
            @PathVariable Long id,
            @RequestBody CMSDto.TestimonialRequest request) {
        return ResponseEntity.ok(cmsService.updateTestimonial(id, request));
    }
    
    @DeleteMapping("/admin/cms/testimonials/{id}")
    public ResponseEntity<Void> deleteTestimonial(@PathVariable Long id) {
        cmsService.deleteTestimonial(id);
        return ResponseEntity.noContent().build();
    }
    
    // Testimonial Links
    @PostMapping("/admin/cms/testimonial-links")
    public ResponseEntity<CMSDto.TestimonialLinkDto> generateTestimonialLink() {
        return ResponseEntity.ok(cmsService.generateTestimonialLink());
    }
    
    @GetMapping("/admin/cms/testimonial-links")
    public ResponseEntity<List<CMSDto.TestimonialLinkDto>> getTestimonialLinks() {
        return ResponseEntity.ok(cmsService.getAllTestimonialLinks());
    }
    
    // Offers
    @GetMapping("/admin/cms/offers")
    public ResponseEntity<List<CMSDto.OfferDto>> getAllOffers() {
        return ResponseEntity.ok(cmsService.getAllOffers());
    }
    
    @PostMapping("/admin/cms/offers")
    public ResponseEntity<CMSDto.OfferDto> createOffer(@RequestBody CMSDto.OfferRequest request) {
        return ResponseEntity.ok(cmsService.createOffer(request));
    }
    
    @PutMapping("/admin/cms/offers/{id}")
    public ResponseEntity<CMSDto.OfferDto> updateOffer(
            @PathVariable Long id,
            @RequestBody CMSDto.OfferRequest request) {
        return ResponseEntity.ok(cmsService.updateOffer(id, request));
    }
    
    @DeleteMapping("/admin/cms/offers/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        cmsService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
    
    // Instagram
    @PutMapping("/admin/cms/instagram")
    public ResponseEntity<Void> setInstagramPosts(@RequestBody CMSDto.InstagramPostRequest request) {
        cmsService.setInstagramPosts(request.getImageUrls());
        return ResponseEntity.ok().build();
    }
    
    // Banners
    @GetMapping("/admin/cms/banners")
    public ResponseEntity<List<CMSDto.BannerDto>> getAllBanners() {
        return ResponseEntity.ok(cmsService.getAllBanners());
    }
    
    @PostMapping("/admin/cms/banners")
    public ResponseEntity<CMSDto.BannerDto> createBanner(@RequestBody CMSDto.BannerRequest request) {
        return ResponseEntity.ok(cmsService.createBanner(request));
    }
    
    @PutMapping("/admin/cms/banners/{id}")
    public ResponseEntity<CMSDto.BannerDto> updateBanner(
            @PathVariable Long id,
            @RequestBody CMSDto.BannerRequest request) {
        return ResponseEntity.ok(cmsService.updateBanner(id, request));
    }
    
    @DeleteMapping("/admin/cms/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        cmsService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
    
    // Landing Content
    @PutMapping("/admin/cms/landing")
    public ResponseEntity<Void> setLandingContent(@RequestBody CMSDto.ContentRequest request) {
        cmsService.setLandingContent(request.getContent());
        return ResponseEntity.ok().build();
    }
    
    // Contact Info
    @PutMapping("/admin/cms/contact")
    public ResponseEntity<Void> setContactInfo(@RequestBody CMSDto.ContentRequest request) {
        cmsService.setContactInfo(request.getContent());
        return ResponseEntity.ok().build();
    }
}
