package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomConfigPricingSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomConfigPricingSlabRepository extends JpaRepository<CustomConfigPricingSlab, Long> {
    List<CustomConfigPricingSlab> findByConfigIdOrderByDisplayOrderAscMinQuantityAsc(Long configId);
    
    // Alternative method name if the above doesn't work
    List<CustomConfigPricingSlab> findByConfigId(Long configId);
}
