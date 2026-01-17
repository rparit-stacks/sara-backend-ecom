package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomProductRepository extends JpaRepository<CustomProduct, Long> {
    
    // Find all saved custom products for a user
    List<CustomProduct> findByUserEmailAndIsSavedTrue(String userEmail);
    
    // Find all custom products for a user (saved and unsaved)
    List<CustomProduct> findByUserEmail(String userEmail);
    
    // Find unsaved custom products older than specified time (for cleanup)
    @Query("SELECT cp FROM CustomProduct cp WHERE cp.isSaved = false AND cp.createdAt < :beforeDate")
    List<CustomProduct> findUnsavedOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
    
    // Delete unsaved custom products older than specified time
    @Modifying
    @Query("DELETE FROM CustomProduct cp WHERE cp.isSaved = false AND cp.createdAt < :beforeDate")
    void deleteUnsavedOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
}
