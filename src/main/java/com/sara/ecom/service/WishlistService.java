package com.sara.ecom.service;

import com.sara.ecom.dto.PlainProductDto;
import com.sara.ecom.dto.ProductDto;
import com.sara.ecom.dto.WishlistDto;
import com.sara.ecom.entity.WishlistItem;
import com.sara.ecom.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    
    @Autowired
    private WishlistItemRepository wishlistItemRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private PlainProductService plainProductService;
    
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
        
        // Populate product details based on product type
        try {
            if (item.getProductType() == WishlistItem.ProductType.PLAIN) {
                PlainProductDto plainProduct = plainProductService.getPlainProductById(item.getProductId());
                dto.setProductName(plainProduct.getName());
                dto.setProductImage(plainProduct.getImage());
                dto.setProductPrice(plainProduct.getPricePerMeter() != null ? 
                    "₹" + plainProduct.getPricePerMeter().toString() : "₹0");
            } else {
                // For DESIGNED or DIGITAL products
                ProductDto product = productService.getProductById(item.getProductId());
                dto.setProductName(product.getName());
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    dto.setProductImage(product.getImages().get(0));
                }
                BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                dto.setProductPrice("₹" + price.toString());
            }
        } catch (Exception e) {
            // If product not found, set defaults
            dto.setProductName("Product not found");
            dto.setProductImage("");
            dto.setProductPrice("₹0");
        }
        
        return dto;
    }
}
