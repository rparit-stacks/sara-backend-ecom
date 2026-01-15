package com.sara.ecom.repository;

import com.sara.ecom.entity.TestimonialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestimonialLinkRepository extends JpaRepository<TestimonialLink, Long> {
    
    Optional<TestimonialLink> findByLinkId(String linkId);
    
    boolean existsByLinkId(String linkId);
}
