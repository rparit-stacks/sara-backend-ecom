package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomProductConfigRepository extends JpaRepository<CustomProductConfig, Long> {
}
