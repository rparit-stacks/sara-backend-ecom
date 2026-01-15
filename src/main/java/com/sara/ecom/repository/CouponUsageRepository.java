package com.sara.ecom.repository;

import com.sara.ecom.entity.Coupon;
import com.sara.ecom.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    
    Optional<CouponUsage> findByCouponAndUserEmail(Coupon coupon, String userEmail);
    
    Integer countByCoupon(Coupon coupon);
}
