package com.sara.ecom.service;

import com.sara.ecom.dto.WishlistDto;
import com.sara.ecom.entity.WishlistItem;
import com.sara.ecom.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    
    @Autowired
    private WishlistItemRepository wishlistItemRepository;
    
    public List<WishlistDto> getWishlist(String userEmail) {
        return wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toWishlistDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public WishlistDto addToWishlist(String userEmail, String productType, Long productId) {
        WishlistItem.ProductType type = WishlistItem.ProductType.valueOf(productType.toUpperCase());
        
        // Check if already exists
        if (wishlistItemRepository.existsByUserEmailAndProductTypeAndProductId(userEmail, type, productId)) {
            throw new RuntimeException("Item already in wishlist");
        }
        
        WishlistItem item = new WishlistItem();
        item.setUserEmail(userEmail);
        item.setProductType(type);
        item.setProductId(productId);
        
        return toWishlistDto(wishlistItemRepository.save(item));
    }
    
    @Transactional
    public void removeFromWishlist(String userEmail, Long itemId) {
        WishlistItem item = wishlistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
        
        if (!item.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to wishlist item");
        }
        
        wishlistItemRepository.delete(item);
    }
    
    @Transactional
    public void removeFromWishlistByProduct(String userEmail, String productType, Long productId) {
        WishlistItem.ProductType type = WishlistItem.ProductType.valueOf(productType.toUpperCase());
        wishlistItemRepository.deleteByUserEmailAndProductTypeAndProductId(userEmail, type, productId);
    }
    
    public boolean isInWishlist(String userEmail, String productType, Long productId) {
        WishlistItem.ProductType type = WishlistItem.ProductType.valueOf(productType.toUpperCase());
        return wishlistItemRepository.existsByUserEmailAndProductTypeAndProductId(userEmail, type, productId);
    }
    
    private WishlistDto toWishlistDto(WishlistItem item) {
        WishlistDto dto = new WishlistDto();
        dto.setId(item.getId());
        dto.setProductType(item.getProductType().name());
        dto.setProductId(item.getProductId());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
