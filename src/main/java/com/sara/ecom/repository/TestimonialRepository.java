package com.sara.ecom.repository;

import com.sara.ecom.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
    
    List<Testimonial> findByIsActiveTrueOrderByCreatedAtDesc();
    
    List<Testimonial> findAllByOrderByCreatedAtDesc();
}
