package com.sara.ecom.repository;

import com.sara.ecom.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    
    void deleteByUserEmail(String userEmail);
    
    long countByUserEmail(String userEmail);
}
