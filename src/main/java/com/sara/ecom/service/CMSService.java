package com.sara.ecom.service;

import com.sara.ecom.dto.CMSDto;
import com.sara.ecom.dto.EmailTemplateData;
import com.sara.ecom.entity.*;
import com.sara.ecom.entity.EmailSubscription;
import com.sara.ecom.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

@Service
public class CMSService {
    
    @Autowired
    private HomepageSectionRepository homepageSectionRepository;
    
    @Autowired
    private TestimonialRepository testimonialRepository;
    
    @Autowired
    private TestimonialLinkRepository testimonialLinkRepository;
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Autowired
    private InstagramPostRepository instagramPostRepository;
    
    @Autowired
    private BannerRepository bannerRepository;
    
    @Autowired
    private CMSContentRepository cmsContentRepository;
    
    @Autowired
    private EmailSubscriptionRepository emailSubscriptionRepository;
    
    @Autowired
    private EmailService emailService;
    
    // Homepage data
    public CMSDto.HomepageResponse getHomepageData() {
        CMSDto.HomepageResponse response = new CMSDto.HomepageResponse();
        
        response.setBestSellerIds(getBestSellerIds());
        response.setNewArrivalIds(getNewArrivalIds());
        response.setTestimonials(getActiveTestimonials());
        response.setOffers(getActiveOffers());
        response.setInstagramPosts(getInstagramPosts());
        response.setBanners(getActiveBanners());
        response.setLandingContent(getLandingContent());
        response.setContactInfo(getContactInfo());
        
        return response;
    }
    
    // Best Sellers
    public List<Long> getBestSellerIds() {
        return homepageSectionRepository.findBySectionTypeOrderByDisplayOrderAsc(HomepageSection.SectionType.BEST_SELLERS)
                .stream().map(HomepageSection::getProductId).collect(Collectors.toList());
    }
    
    @Transactional
    public void setBestSellers(List<Long> productIds) {
        homepageSectionRepository.deleteBySectionType(HomepageSection.SectionType.BEST_SELLERS);
        int order = 0;
        for (Long productId : productIds) {
            HomepageSection section = new HomepageSection();
            section.setSectionType(HomepageSection.SectionType.BEST_SELLERS);
            section.setProductId(productId);
            section.setDisplayOrder(order++);
            homepageSectionRepository.save(section);
        }
    }
    
    // New Arrivals
    public List<Long> getNewArrivalIds() {
        return homepageSectionRepository.findBySectionTypeOrderByDisplayOrderAsc(HomepageSection.SectionType.NEW_ARRIVALS)
                .stream().map(HomepageSection::getProductId).collect(Collectors.toList());
    }
    
    @Transactional
    public void setNewArrivals(List<Long> productIds) {
        homepageSectionRepository.deleteBySectionType(HomepageSection.SectionType.NEW_ARRIVALS);
        int order = 0;
        for (Long productId : productIds) {
            HomepageSection section = new HomepageSection();
            section.setSectionType(HomepageSection.SectionType.NEW_ARRIVALS);
            section.setProductId(productId);
            section.setDisplayOrder(order++);
            homepageSectionRepository.save(section);
        }
    }
    
    // Testimonials
    public List<CMSDto.TestimonialDto> getActiveTestimonials() {
        return testimonialRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .limit(10)
                .map(this::toTestimonialDto)
                .collect(Collectors.toList());
    }
    
    public List<CMSDto.TestimonialDto> getAllTestimonials() {
        return testimonialRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTestimonialDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CMSDto.TestimonialDto createTestimonial(CMSDto.TestimonialRequest request) {
        Testimonial testimonial = new Testimonial();
        testimonial.setName(request.getName());
        testimonial.setText(request.getText());
        testimonial.setRating(request.getRating() != null ? request.getRating() : 5);
        testimonial.setLocation(request.getLocation());
        testimonial.setIsActive(request.getIsActive() != null ? request.getIsActive() : false);
        return toTestimonialDto(testimonialRepository.save(testimonial));
    }
    
    @Transactional
    public CMSDto.TestimonialDto updateTestimonial(Long id, CMSDto.TestimonialRequest request) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testimonial not found"));
        testimonial.setName(request.getName());
        testimonial.setText(request.getText());
        testimonial.setRating(request.getRating());
        testimonial.setLocation(request.getLocation());
        testimonial.setIsActive(request.getIsActive());
        return toTestimonialDto(testimonialRepository.save(testimonial));
    }
    
    @Transactional
    public void deleteTestimonial(Long id) {
        testimonialRepository.deleteById(id);
    }
    
    // Testimonial Links
    @Transactional
    public CMSDto.TestimonialLinkDto generateTestimonialLink() {
        String linkId = "testimonial-" + System.currentTimeMillis();
        TestimonialLink link = new TestimonialLink();
        link.setLinkId(linkId);
        TestimonialLink saved = testimonialLinkRepository.save(link);
        return toTestimonialLinkDto(saved);
    }
    
    public List<CMSDto.TestimonialLinkDto> getAllTestimonialLinks() {
        return testimonialLinkRepository.findAll().stream()
                .map(this::toTestimonialLinkDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CMSDto.TestimonialDto submitTestimonial(String linkId, CMSDto.TestimonialSubmitRequest request) {
        TestimonialLink link = testimonialLinkRepository.findByLinkId(linkId)
                .orElseThrow(() -> new RuntimeException("Invalid testimonial link"));
        
        if (link.getIsUsed()) {
            throw new RuntimeException("This link has already been used");
        }
        
        Testimonial testimonial = new Testimonial();
        testimonial.setName(request.getName());
        testimonial.setText(request.getText());
        testimonial.setRating(request.getRating() != null ? request.getRating() : 5);
        testimonial.setLocation(request.getLocation());
        testimonial.setIsActive(false); // Admin needs to approve
        
        // Delete the link instead of marking it as used
        testimonialLinkRepository.delete(link);
        
        return toTestimonialDto(testimonialRepository.save(testimonial));
    }
    
    // Offers
    public List<CMSDto.OfferDto> getActiveOffers() {
        return offerRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toOfferDto)
                .collect(Collectors.toList());
    }
    
    public List<CMSDto.OfferDto> getAllOffers() {
        return offerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toOfferDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CMSDto.OfferDto createOffer(CMSDto.OfferRequest request) {
        Offer offer = new Offer();
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return toOfferDto(offerRepository.save(offer));
    }
    
    @Transactional
    public CMSDto.OfferDto updateOffer(Long id, CMSDto.OfferRequest request) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setIsActive(request.getIsActive());
        return toOfferDto(offerRepository.save(offer));
    }
    
    @Transactional
    public void deleteOffer(Long id) {
        offerRepository.deleteById(id);
    }
    
    // Instagram Posts
    public List<CMSDto.InstagramPostDto> getInstagramPosts() {
        return instagramPostRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(post -> {
                    CMSDto.InstagramPostDto dto = new CMSDto.InstagramPostDto();
                    dto.setImageUrl(post.getImageUrl());
                    dto.setLinkUrl(post.getLinkUrl());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void setInstagramPosts(List<CMSDto.InstagramPostItem> posts) {
        instagramPostRepository.deleteAll();
        int order = 0;
        for (CMSDto.InstagramPostItem item : posts) {
            InstagramPost post = new InstagramPost();
            post.setImageUrl(item.getImageUrl());
            post.setLinkUrl(item.getLinkUrl());
            post.setDisplayOrder(order++);
            instagramPostRepository.save(post);
        }
    }
    
    // Banners
    public List<CMSDto.BannerDto> getActiveBanners() {
        return bannerRepository.findActiveBanners(LocalDateTime.now()).stream()
                .map(this::toBannerDto)
                .collect(Collectors.toList());
    }
    
    public List<CMSDto.BannerDto> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toBannerDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CMSDto.BannerDto createBanner(CMSDto.BannerRequest request) {
        Banner banner = new Banner();
        mapBannerRequest(request, banner);
        return toBannerDto(bannerRepository.save(banner));
    }
    
    @Transactional
    public CMSDto.BannerDto updateBanner(Long id, CMSDto.BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));
        mapBannerRequest(request, banner);
        return toBannerDto(bannerRepository.save(banner));
    }
    
    @Transactional
    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }
    
    // Landing Content
    public Map<String, String> getLandingContent() {
        return cmsContentRepository.findByKeyStartingWith("landing.").stream()
                .collect(Collectors.toMap(
                        c -> c.getKey().replace("landing.", ""),
                        CMSContent::getValue
                ));
    }
    
    @Transactional
    public void setLandingContent(Map<String, String> content) {
        for (Map.Entry<String, String> entry : content.entrySet()) {
            String key = "landing." + entry.getKey();
            CMSContent cmsContent = cmsContentRepository.findByKey(key)
                    .orElse(new CMSContent());
            cmsContent.setKey(key);
            cmsContent.setValue(entry.getValue());
            cmsContentRepository.save(cmsContent);
        }
    }
    
    // Contact Info
    public Map<String, String> getContactInfo() {
        return cmsContentRepository.findByKeyStartingWith("contact.").stream()
                .collect(Collectors.toMap(
                        c -> c.getKey().replace("contact.", ""),
                        CMSContent::getValue
                ));
    }
    
    @Transactional
    public void setContactInfo(Map<String, String> content) {
        for (Map.Entry<String, String> entry : content.entrySet()) {
            String key = "contact." + entry.getKey();
            CMSContent cmsContent = cmsContentRepository.findByKey(key)
                    .orElse(new CMSContent());
            cmsContent.setKey(key);
            cmsContent.setValue(entry.getValue());
            cmsContentRepository.save(cmsContent);
        }
    }
    
    // Mappers
    private void mapBannerRequest(CMSDto.BannerRequest request, Banner banner) {
        banner.setImage(request.getImage());
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setLink(request.getLink());
        banner.setButtonText(request.getButtonText());
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        banner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
    }
    
    private CMSDto.TestimonialDto toTestimonialDto(Testimonial t) {
        CMSDto.TestimonialDto dto = new CMSDto.TestimonialDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setText(t.getText());
        dto.setRating(t.getRating());
        dto.setLocation(t.getLocation());
        dto.setIsActive(t.getIsActive());
        return dto;
    }
    
    private CMSDto.TestimonialLinkDto toTestimonialLinkDto(TestimonialLink link) {
        CMSDto.TestimonialLinkDto dto = new CMSDto.TestimonialLinkDto();
        dto.setId(link.getId());
        dto.setLinkId(link.getLinkId());
        dto.setIsUsed(link.getIsUsed());
        dto.setCreatedAt(link.getCreatedAt());
        // Set full link URL (frontend will construct it, but we can provide it for convenience)
        // Note: In production, you might want to get this from configuration
        String baseUrl = System.getenv().getOrDefault("FRONTEND_BASE_URL", "https://www.studiosara.in");
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        dto.setFullLink(baseUrl + "/testimonial/" + link.getLinkId());
        return dto;
    }
    
    private CMSDto.OfferDto toOfferDto(Offer o) {
        CMSDto.OfferDto dto = new CMSDto.OfferDto();
        dto.setId(o.getId());
        dto.setTitle(o.getTitle());
        dto.setDescription(o.getDescription());
        dto.setIsActive(o.getIsActive());
        return dto;
    }
    
    // Get Instagram Thumbnail
    public String getInstagramThumbnail(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("URL is required");
        }
        
        // If it's already a direct image URL, return it
        if (url.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return url;
        }
        
        // Extract shortcode from Instagram URL
        // Supports formats like:
        // https://www.instagram.com/p/ABC123/
        // https://instagram.com/p/ABC123/
        // https://www.instagram.com/reel/ABC123/
        Pattern pattern = Pattern.compile("instagram\\.com/(?:p|reel)/([A-Za-z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            String shortcode = matcher.group(1);
            // Use Instagram's media endpoint to get the image
            // This works for most public posts
            return "https://www.instagram.com/p/" + shortcode + "/media/?size=l";
        }
        
        // If not an Instagram URL, try to fetch og:image from the page
        try {
            return fetchOgImage(url);
        } catch (Exception e) {
            // If all else fails, return the original URL
            return url;
        }
    }
    
    private String fetchOgImage(String url) throws Exception {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
            reader.close();
            
            // Extract og:image
            Pattern ogImagePattern = Pattern.compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher ogMatcher = ogImagePattern.matcher(html.toString());
            if (ogMatcher.find()) {
                return ogMatcher.group(1);
            }
            
            // Try alternate format
            Pattern ogImagePattern2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);
            Matcher ogMatcher2 = ogImagePattern2.matcher(html.toString());
            if (ogMatcher2.find()) {
                return ogMatcher2.group(1);
            }
            
            throw new RuntimeException("Could not find og:image in page");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch thumbnail: " + e.getMessage());
        }
    }
    
    // Email Subscription
    @Transactional
    public void subscribeEmail(String email) {
        // Normalize email to lowercase
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        
        // Check if email already exists
        if (emailSubscriptionRepository.existsByEmail(normalizedEmail)) {
            // Update existing subscription to active if it was inactive
            EmailSubscription existing = emailSubscriptionRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new RuntimeException("Email subscription not found"));
            if (!existing.getIsActive()) {
                existing.setIsActive(true);
                emailSubscriptionRepository.save(existing);
            }
            // If already active, just return (no error)
            return;
        }
        
        // Create new subscription
        EmailSubscription subscription = new EmailSubscription();
        subscription.setEmail(normalizedEmail);
        subscription.setIsActive(true);
        emailSubscriptionRepository.save(subscription);
        
        // Send email notification
        try {
            EmailTemplateData.NewsletterSubscriptionData emailData = new EmailTemplateData.NewsletterSubscriptionData();
            emailData.setRecipientName(normalizedEmail);
            emailData.setRecipientEmail(normalizedEmail);
            
            emailService.sendNewsletterSubscriptionEmail(emailData);
        } catch (Exception e) {
            // Log error but don't fail subscription
            System.err.println("Failed to send newsletter subscription email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private CMSDto.BannerDto toBannerDto(Banner b) {
        CMSDto.BannerDto dto = new CMSDto.BannerDto();
        dto.setId(b.getId());
        dto.setImage(b.getImage());
        dto.setTitle(b.getTitle());
        dto.setSubtitle(b.getSubtitle());
        dto.setLink(b.getLink());
        dto.setButtonText(b.getButtonText());
        dto.setStartDate(b.getStartDate());
        dto.setEndDate(b.getEndDate());
        dto.setIsActive(b.getIsActive());
        dto.setDisplayOrder(b.getDisplayOrder());
        return dto;
    }
}
