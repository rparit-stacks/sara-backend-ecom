package com.sara.ecom.repository;

import com.sara.ecom.entity.ShippingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingRuleRepository extends JpaRepository<ShippingRule, Long> {
    
    List<ShippingRule> findByIsActiveTrueOrderByPriorityDesc();
    
    List<ShippingRule> findByScopeAndIsActiveTrueOrderByPriorityDesc(ShippingRule.Scope scope);
    
    List<ShippingRule> findByScopeAndStateAndIsActiveTrueOrderByPriorityDesc(
        ShippingRule.Scope scope, 
        String state
    );
    
    @Query("SELECT r FROM ShippingRule r LEFT JOIN FETCH r.ranges WHERE r.isActive = true ORDER BY r.priority DESC")
    List<ShippingRule> findAllActiveWithRanges();
}
