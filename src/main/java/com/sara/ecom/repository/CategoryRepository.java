package com.sara.ecom.repository;

import com.sara.ecom.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByParentIdIsNull();
    
    List<Category> findByParentId(Long parentId);
    
    List<Category> findByStatus(Category.Status status);
    
    List<Category> findByParentIdIsNullAndStatus(Category.Status status);
    
    Optional<Category> findByIdAndStatus(Long id, Category.Status status);
    
    @Query("SELECT c FROM Category c WHERE c.status = 'ACTIVE' ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllActiveOrdered();
    
    boolean existsByNameAndParentId(String name, Long parentId);
    
    boolean existsByNameAndParentIdAndIdNot(String name, Long parentId, Long id);
    
    Optional<Category> findBySlug(String slug);
    
    Optional<Category> findBySlugAndStatus(String slug, Category.Status status);
    
    boolean existsBySlug(String slug);
    
    boolean existsBySlugAndIdNot(String slug, Long id);
    
    List<Category> findByParentIdAndStatus(Long parentId, Category.Status status);
}
