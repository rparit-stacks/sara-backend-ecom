package com.sara.ecom.repository;

import com.sara.ecom.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    List<Offer> findByIsActiveTrueOrderByCreatedAtDesc();
    
    List<Offer> findAllByOrderByCreatedAtDesc();
}
