package com.sara.ecom.service;

import com.sara.ecom.entity.CustomProduct;
import com.sara.ecom.repository.CustomProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomProductService {
    
    private final CustomProductRepository customProductRepository;
    
    /**
     * Saves a custom product. If isSaved is false, it will be auto-deleted after 24 hours.
     */
    @Transactional
    public CustomProduct saveCustomProduct(CustomProduct customProduct) {
        return customProductRepository.save(customProduct);
    }
    
    /**
     * Gets all saved custom products for a user.
     */
    @Transactional(readOnly = true)
    public List<CustomProduct> getSavedCustomProducts(String userEmail) {
        return customProductRepository.findByUserEmailAndIsSavedTrue(userEmail);
    }
    
    /**
     * Gets all custom products for a user (saved and unsaved).
     */
    @Transactional(readOnly = true)
    public List<CustomProduct> getAllCustomProducts(String userEmail) {
        return customProductRepository.findByUserEmail(userEmail);
    }
    
    /**
     * Marks a custom product as saved.
     */
    @Transactional
    public CustomProduct saveCustomProduct(Long id, String userEmail) {
        CustomProduct customProduct = customProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom product not found"));
        
        // Verify ownership
        if (!customProduct.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You can only save your own custom products");
        }
        
        customProduct.setIsSaved(true);
        return customProductRepository.save(customProduct);
    }
    
    /**
     * Deletes a custom product.
     */
    @Transactional
    public void deleteCustomProduct(Long id, String userEmail) {
        CustomProduct customProduct = customProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom product not found"));
        
        // Verify ownership
        if (!customProduct.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You can only delete your own custom products");
        }
        
        customProductRepository.delete(customProduct);
    }
    
    /**
     * Deletes an unsaved custom product (when user leaves/cancels).
     */
    @Transactional
    public void deleteUnsavedCustomProduct(Long id, String userEmail) {
        CustomProduct customProduct = customProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom product not found"));
        
        // Verify ownership
        if (!customProduct.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You can only delete your own custom products");
        }
        
        // Only delete if not saved
        if (!customProduct.getIsSaved()) {
            customProductRepository.delete(customProduct);
        }
    }
    
    /**
     * Gets a custom product by ID (only if user owns it).
     */
    @Transactional(readOnly = true)
    public CustomProduct getCustomProductById(Long id, String userEmail) {
        CustomProduct customProduct = customProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom product not found"));
        
        // Verify ownership
        if (!customProduct.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You can only access your own custom products");
        }
        
        return customProduct;
    }
    
    /**
     * Scheduled task to delete unsaved custom products older than 24 hours.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void cleanupUnsavedCustomProducts() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<CustomProduct> unsavedProducts = customProductRepository.findUnsavedOlderThan(twentyFourHoursAgo);
        
        if (!unsavedProducts.isEmpty()) {
            customProductRepository.deleteUnsavedOlderThan(twentyFourHoursAgo);
            // Log if needed
            System.out.println("Cleaned up " + unsavedProducts.size() + " unsaved custom products");
        }
    }
}
