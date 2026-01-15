package com.sara.ecom.service;

import com.sara.ecom.dto.ShippingRuleDto;
import com.sara.ecom.dto.ShippingRuleRequest;
import com.sara.ecom.entity.ShippingRange;
import com.sara.ecom.entity.ShippingRule;
import com.sara.ecom.repository.ShippingRangeRepository;
import com.sara.ecom.repository.ShippingRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingService {
    
    @Autowired
    private ShippingRuleRepository shippingRuleRepository;
    
    @Autowired
    private ShippingRangeRepository shippingRangeRepository;
    
    /**
     * Calculate shipping charge based on cart value and state
     * Priority: State+Range > State+Flat > AllIndia+Range > AllIndia+Flat
     */
    public BigDecimal calculateShipping(BigDecimal cartValue, String state) {
        if (cartValue == null || cartValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Get all active rules with ranges loaded
        List<ShippingRule> allRules = shippingRuleRepository.findAllActiveWithRanges();
        
        // Priority 1: State-wise + Range-based
        if (state != null && !state.trim().isEmpty()) {
            ShippingRule stateRangeRule = allRules.stream()
                .filter(r -> r.getScope() == ShippingRule.Scope.STATE_WISE)
                .filter(r -> state.equalsIgnoreCase(r.getState()))
                .filter(r -> r.getCalculationType() == ShippingRule.CalculationType.RANGE_BASED)
                .filter(r -> r.getRanges() != null && !r.getRanges().isEmpty())
                .max(Comparator.comparing(ShippingRule::getPriority))
                .orElse(null);
            
            if (stateRangeRule != null) {
                BigDecimal shipping = calculateRangeBasedShipping(stateRangeRule, cartValue);
                if (shipping != null) {
                    return shipping;
                }
            }
            
            // Priority 2: State-wise + Flat
            ShippingRule stateFlatRule = allRules.stream()
                .filter(r -> r.getScope() == ShippingRule.Scope.STATE_WISE)
                .filter(r -> state.equalsIgnoreCase(r.getState()))
                .filter(r -> r.getCalculationType() == ShippingRule.CalculationType.FLAT)
                .max(Comparator.comparing(ShippingRule::getPriority))
                .orElse(null);
            
            if (stateFlatRule != null) {
                // Check free shipping threshold
                if (stateFlatRule.getFreeShippingAbove() != null && 
                    cartValue.compareTo(stateFlatRule.getFreeShippingAbove()) >= 0) {
                    return BigDecimal.ZERO;
                }
                return stateFlatRule.getFlatPrice() != null ? stateFlatRule.getFlatPrice() : BigDecimal.ZERO;
            }
        }
        
        // Priority 3: All India + Range-based
        ShippingRule allIndiaRangeRule = allRules.stream()
            .filter(r -> r.getScope() == ShippingRule.Scope.ALL_INDIA)
            .filter(r -> r.getCalculationType() == ShippingRule.CalculationType.RANGE_BASED)
            .filter(r -> r.getRanges() != null && !r.getRanges().isEmpty())
            .max(Comparator.comparing(ShippingRule::getPriority))
            .orElse(null);
        
        if (allIndiaRangeRule != null) {
            BigDecimal shipping = calculateRangeBasedShipping(allIndiaRangeRule, cartValue);
            if (shipping != null) {
                return shipping;
            }
        }
        
        // Priority 4: All India + Flat
        ShippingRule allIndiaFlatRule = allRules.stream()
            .filter(r -> r.getScope() == ShippingRule.Scope.ALL_INDIA)
            .filter(r -> r.getCalculationType() == ShippingRule.CalculationType.FLAT)
            .max(Comparator.comparing(ShippingRule::getPriority))
            .orElse(null);
        
        if (allIndiaFlatRule != null) {
            // Check free shipping threshold
            if (allIndiaFlatRule.getFreeShippingAbove() != null && 
                cartValue.compareTo(allIndiaFlatRule.getFreeShippingAbove()) >= 0) {
                return BigDecimal.ZERO;
            }
            return allIndiaFlatRule.getFlatPrice() != null ? allIndiaFlatRule.getFlatPrice() : BigDecimal.ZERO;
        }
        
        // No rule matched - free shipping
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateRangeBasedShipping(ShippingRule rule, BigDecimal cartValue) {
        // Check free shipping threshold first
        if (rule.getFreeShippingAbove() != null && 
            cartValue.compareTo(rule.getFreeShippingAbove()) >= 0) {
            return BigDecimal.ZERO;
        }
        
        // Find matching range
        if (rule.getRanges() != null) {
            // Sort ranges by display order
            List<ShippingRange> sortedRanges = rule.getRanges().stream()
                .sorted(Comparator.comparing(ShippingRange::getDisplayOrder))
                .collect(Collectors.toList());
            
            for (ShippingRange range : sortedRanges) {
                if (range.matches(cartValue)) {
                    return range.getShippingPrice();
                }
            }
        }
        
        return null; // No matching range
    }
    
    // Admin methods
    public List<ShippingRuleDto> getAllShippingRules() {
        return shippingRuleRepository.findAll().stream()
                .map(this::toShippingRuleDto)
                .collect(Collectors.toList());
    }
    
    public ShippingRuleDto getShippingRuleById(Long id) {
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipping rule not found"));
        return toShippingRuleDto(rule);
    }
    
    @Transactional
    public ShippingRuleDto createShippingRule(ShippingRuleRequest request) {
        ShippingRule rule = new ShippingRule();
        mapRequestToRule(request, rule);
        
        // Save ranges
        if (request.getRanges() != null) {
            for (ShippingRuleRequest.ShippingRangeRequest rangeReq : request.getRanges()) {
                ShippingRange range = new ShippingRange();
                range.setMinCartValue(rangeReq.getMinCartValue());
                range.setMaxCartValue(rangeReq.getMaxCartValue());
                range.setShippingPrice(rangeReq.getShippingPrice());
                range.setDisplayOrder(rangeReq.getDisplayOrder() != null ? rangeReq.getDisplayOrder() : 0);
                rule.addRange(range);
            }
        }
        
        ShippingRule saved = shippingRuleRepository.save(rule);
        return toShippingRuleDto(saved);
    }
    
    @Transactional
    public ShippingRuleDto updateShippingRule(Long id, ShippingRuleRequest request) {
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipping rule not found"));
        
        // Clear existing ranges
        List<ShippingRange> existingRanges = shippingRangeRepository.findByShippingRuleIdOrderByDisplayOrderAsc(id);
        shippingRangeRepository.deleteAll(existingRanges);
        rule.getRanges().clear();
        
        mapRequestToRule(request, rule);
        
        // Add new ranges
        if (request.getRanges() != null) {
            for (ShippingRuleRequest.ShippingRangeRequest rangeReq : request.getRanges()) {
                ShippingRange range = new ShippingRange();
                range.setMinCartValue(rangeReq.getMinCartValue());
                range.setMaxCartValue(rangeReq.getMaxCartValue());
                range.setShippingPrice(rangeReq.getShippingPrice());
                range.setDisplayOrder(rangeReq.getDisplayOrder() != null ? rangeReq.getDisplayOrder() : 0);
                rule.addRange(range);
            }
        }
        
        ShippingRule saved = shippingRuleRepository.save(rule);
        return toShippingRuleDto(saved);
    }
    
    @Transactional
    public void deleteShippingRule(Long id) {
        shippingRuleRepository.deleteById(id);
    }
    
    private void mapRequestToRule(ShippingRuleRequest request, ShippingRule rule) {
        rule.setRuleName(request.getRuleName());
        rule.setScope(ShippingRule.Scope.valueOf(request.getScope().toUpperCase()));
        rule.setState(request.getState());
        rule.setCalculationType(ShippingRule.CalculationType.valueOf(request.getCalculationType().toUpperCase()));
        rule.setFlatPrice(request.getFlatPrice());
        rule.setFreeShippingAbove(request.getFreeShippingAbove());
        rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
    }
    
    private ShippingRuleDto toShippingRuleDto(ShippingRule rule) {
        ShippingRuleDto dto = new ShippingRuleDto();
        dto.setId(rule.getId());
        dto.setRuleName(rule.getRuleName());
        dto.setScope(rule.getScope().name());
        dto.setState(rule.getState());
        dto.setCalculationType(rule.getCalculationType().name());
        dto.setFlatPrice(rule.getFlatPrice());
        dto.setFreeShippingAbove(rule.getFreeShippingAbove());
        dto.setIsActive(rule.getIsActive());
        dto.setPriority(rule.getPriority());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        
        if (rule.getRanges() != null) {
            dto.setRanges(rule.getRanges().stream()
                .sorted(Comparator.comparing(ShippingRange::getDisplayOrder))
                .map(this::toShippingRangeDto)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private ShippingRuleDto.ShippingRangeDto toShippingRangeDto(ShippingRange range) {
        ShippingRuleDto.ShippingRangeDto dto = new ShippingRuleDto.ShippingRangeDto();
        dto.setId(range.getId());
        dto.setMinCartValue(range.getMinCartValue());
        dto.setMaxCartValue(range.getMaxCartValue());
        dto.setShippingPrice(range.getShippingPrice());
        dto.setDisplayOrder(range.getDisplayOrder());
        return dto;
    }
}
