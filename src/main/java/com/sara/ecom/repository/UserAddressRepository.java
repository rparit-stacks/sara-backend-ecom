package com.sara.ecom.repository;

import com.sara.ecom.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    
    List<UserAddress> findByUserEmailOrderByIsDefaultDescCreatedAtDesc(String userEmail);
    
    Optional<UserAddress> findByIdAndUserEmail(Long id, String userEmail);
    
    Optional<UserAddress> findByUserEmailAndIsDefaultTrue(String userEmail);
    
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.userEmail = :userEmail")
    void clearDefaultAddresses(@Param("userEmail") String userEmail);
    
    long countByUserEmail(String userEmail);
}
