package com.sara.ecom.repository;

import com.sara.ecom.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {
    
    List<Design> findByStatus(Design.Status status);
    
    List<Design> findByCategory(String category);
    
    List<Design> findByStatusAndCategory(Design.Status status, String category);
    
    @Query("SELECT d FROM Design d LEFT JOIN FETCH d.assignedFabrics WHERE d.id = :id")
    Optional<Design> findByIdWithFabrics(@Param("id") Long id);
    
    @Query("SELECT DISTINCT d FROM Design d LEFT JOIN FETCH d.assignedFabrics WHERE d.status = :status")
    List<Design> findAllWithFabricsByStatus(@Param("status") Design.Status status);
    
    @Query("SELECT DISTINCT d FROM Design d LEFT JOIN FETCH d.assignedFabrics")
    List<Design> findAllWithFabrics();
    
    @Query("SELECT DISTINCT d.category FROM Design d WHERE d.category IS NOT NULL")
    List<String> findAllCategories();
}
