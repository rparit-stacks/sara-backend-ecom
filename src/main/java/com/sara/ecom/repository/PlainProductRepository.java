package com.sara.ecom.repository;

import com.sara.ecom.entity.PlainProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlainProductRepository extends JpaRepository<PlainProduct, Long> {
    
    List<PlainProduct> findByStatus(PlainProduct.Status status);
    
    List<PlainProduct> findByCategoryId(Long categoryId);
    
    List<PlainProduct> findByStatusAndCategoryId(PlainProduct.Status status, Long categoryId);
    
    @Query("SELECT DISTINCT p FROM PlainProduct p LEFT JOIN FETCH p.variants v WHERE p.id = :id")
    Optional<PlainProduct> findByIdWithVariants(@Param("id") Long id);
    
    @Query("SELECT DISTINCT p FROM PlainProduct p LEFT JOIN FETCH p.variants v WHERE p.status = :status")
    List<PlainProduct> findAllWithVariantsByStatus(@Param("status") PlainProduct.Status status);
    
    @Query("SELECT DISTINCT p FROM PlainProduct p LEFT JOIN FETCH p.variants v")
    List<PlainProduct> findAllWithVariants();
    
    List<PlainProduct> findByIdIn(List<Long> ids);
}
