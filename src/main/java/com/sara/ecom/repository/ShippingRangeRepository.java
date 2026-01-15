package com.sara.ecom.repository;

import com.sara.ecom.entity.ShippingRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingRangeRepository extends JpaRepository<ShippingRange, Long> {
    
    List<ShippingRange> findByShippingRuleIdOrderByDisplayOrderAsc(Long shippingRuleId);
    
    @Modifying
    @Query("DELETE FROM ShippingRange r WHERE r.shippingRule.id = :ruleId")
    void deleteByShippingRuleId(@Param("ruleId") Long ruleId);
}
