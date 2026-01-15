package com.sara.ecom.repository;

import com.sara.ecom.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<Banner> findAllByOrderByDisplayOrderAsc();
    
    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND (b.startDate IS NULL OR b.startDate <= :now) AND (b.endDate IS NULL OR b.endDate >= :now) ORDER BY b.displayOrder ASC")
    List<Banner> findActiveBanners(LocalDateTime now);
}
