package com.sara.ecom.repository;

import com.sara.ecom.entity.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {
    
    List<FAQ> findByStatusOrderByDisplayOrderAsc(FAQ.Status status);
    
    List<FAQ> findByCategoryAndStatusOrderByDisplayOrderAsc(String category, FAQ.Status status);
    
    List<FAQ> findAllByOrderByDisplayOrderAsc();
    
    @Query("SELECT DISTINCT f.category FROM FAQ f WHERE f.category IS NOT NULL")
    List<String> findAllCategories();
}
