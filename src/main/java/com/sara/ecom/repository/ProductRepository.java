package com.sara.ecom.repository;

import com.sara.ecom.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByStatus(Product.Status status);
    
    List<Product> findByType(Product.ProductType type);
    
    List<Product> findByStatusAndType(Product.Status status, Product.ProductType type);
    
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByStatusAndCategoryId(Product.Status status, Long categoryId);
    
    List<Product> findByTypeAndCategoryId(Product.ProductType type, Long categoryId);
    
    List<Product> findByStatusAndTypeAndCategoryId(Product.Status status, Product.ProductType type, Long categoryId);
    
    // Split into separate queries to avoid MultipleBagFetchException
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);
    
    // Separate query for detail sections
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.detailSections WHERE p.id = :id")
    Optional<Product> findByIdWithDetailSections(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.customFields WHERE p.id = :id")
    Optional<Product> findByIdWithCustomFields(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);
    
    // Find by slug
    Optional<Product> findBySlug(String slug);
    
    // Find by slug with status
    Optional<Product> findBySlugAndStatus(String slug, Product.Status status);
    
    boolean existsBySlug(String slug);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.status = :status")
    List<Product> findAllWithImagesByStatus(@Param("status") Product.Status status);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images")
    List<Product> findAllWithImages();
    
    List<Product> findByIdIn(List<Long> ids);
    
    List<Product> findByIsNewTrue();
    
    List<Product> findByIsSaleTrue();
    
    @Query("SELECT p FROM Product p WHERE p.isNew = true AND p.status = :status")
    List<Product> findNewProducts(@Param("status") Product.Status status);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.categoryId IN :categoryIds AND p.status = :status")
    long countActiveProductsByCategoryIds(@Param("categoryIds") List<Long> categoryIds, @Param("status") Product.Status status);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.categoryId = :categoryId AND p.status = :status")
    long countActiveProductsByCategoryId(@Param("categoryId") Long categoryId, @Param("status") Product.Status status);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.categoryId IN :categoryIds AND p.status = :status")
    List<Product> findByCategoryIdsAndStatus(@Param("categoryIds") List<Long> categoryIds, @Param("status") Product.Status status);
    
    // Find Digital Product created from a Design Product
    Optional<Product> findBySourceDesignProductId(Long sourceDesignProductId);
}
