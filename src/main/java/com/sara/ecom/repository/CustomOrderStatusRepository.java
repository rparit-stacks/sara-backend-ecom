package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomOrderStatusRepository extends JpaRepository<CustomOrderStatus, Long> {
    Optional<CustomOrderStatus> findByStatusName(String statusName);
    List<CustomOrderStatus> findByIsActiveTrue();
}
