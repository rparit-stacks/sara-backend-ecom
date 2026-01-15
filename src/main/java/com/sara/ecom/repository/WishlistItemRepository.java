package com.sara.ecom.repository;

import com.sara.ecom.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    
    List<WishlistItem> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    
    Optional<WishlistItem> findByUserEmailAndProductTypeAndProductId(
            String userEmail, WishlistItem.ProductType productType, Long productId);
    
    boolean existsByUserEmailAndProductTypeAndProductId(
            String userEmail, WishlistItem.ProductType productType, Long productId);
    
    void deleteByUserEmailAndProductTypeAndProductId(
            String userEmail, WishlistItem.ProductType productType, Long productId);
}
