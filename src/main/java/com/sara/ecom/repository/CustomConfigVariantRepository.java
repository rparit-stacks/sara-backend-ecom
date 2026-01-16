package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomConfigVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomConfigVariantRepository extends JpaRepository<CustomConfigVariant, Long> {
    List<CustomConfigVariant> findByConfigIdOrderByDisplayOrderAsc(Long configId);
    
    @Query("SELECT DISTINCT v FROM CustomConfigVariant v LEFT JOIN FETCH v.options WHERE v.config.id = :configId ORDER BY v.displayOrder ASC")
    List<CustomConfigVariant> findByConfigIdWithOptions(@Param("configId") Long configId);
}
