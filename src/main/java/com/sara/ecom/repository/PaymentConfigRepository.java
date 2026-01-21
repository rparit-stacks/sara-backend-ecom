package com.sara.ecom.repository;

import com.sara.ecom.entity.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, Long> {
    Optional<PaymentConfig> findFirstByOrderByIdAsc();
}
