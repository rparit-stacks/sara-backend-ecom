package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomDesignRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomDesignRequestRepository extends JpaRepository<CustomDesignRequest, Long> {
    
    List<CustomDesignRequest> findAllByOrderByCreatedAtDesc();
    
    List<CustomDesignRequest> findByStatusOrderByCreatedAtDesc(CustomDesignRequest.Status status);
}
