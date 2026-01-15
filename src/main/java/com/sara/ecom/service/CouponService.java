package com.sara.ecom.service;

import com.sara.ecom.dto.CouponDto;
import com.sara.ecom.dto.CouponRequest;
import com.sara.ecom.entity.Coupon;
import com.sara.ecom.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {
    
    @Autowired
    private CouponRepository couponRepository;
    
    public CouponDto validateCoupon(String code, BigDecimal orderTotal) {
        CouponDto response = new CouponDto();
        response.setCode(code);
        
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code).orElse(null);
        
        if (coupon == null) {
            response.setValid(false);
            response.setMessage("Invalid coupon code");
            return response;
        }
        
        if (!coupon.getIsActive()) {
            response.setValid(false);
            response.setMessage("This coupon is no longer active");
            return response;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            response.setValid(false);
            response.setMessage("This coupon is not yet valid");
            return response;
        }
        
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            response.setValid(false);
            response.setMessage("This coupon has expired");
            return response;
        }
        
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            response.setValid(false);
            response.setMessage("This coupon has reached its usage limit");
            return response;
        }
        
        if (coupon.getMinOrder() != null && orderTotal.compareTo(coupon.getMinOrder()) < 0) {
            response.setValid(false);
            response.setMessage("Minimum order value of ₹" + coupon.getMinOrder() + " required");
            return response;
        }
        
        // Calculate discount
        BigDecimal discount;
        if (coupon.getType() == Coupon.CouponType.PERCENTAGE) {
            discount = orderTotal.multiply(coupon.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getValue();
        }
        
        response.setValid(true);
        response.setMessage("Coupon applied successfully");
        response.setDiscount(discount);
        response.setType(coupon.getType().name());
        response.setValue(coupon.getValue());
        
        return response;
    }
    
    @Transactional
    public void useCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.incrementUsedCount();
        couponRepository.save(coupon);
    }
    
    // Admin methods
    public List<CouponDto> getAllCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCouponDto)
                .collect(Collectors.toList());
    }
    
    public CouponDto getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        return toCouponDto(coupon);
    }
    
    @Transactional
    public CouponDto createCoupon(CouponRequest request) {
        Coupon coupon = new Coupon();
        mapRequestToCoupon(request, coupon);
        return toCouponDto(couponRepository.save(coupon));
    }
    
    @Transactional
    public CouponDto updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        mapRequestToCoupon(request, coupon);
        return toCouponDto(couponRepository.save(coupon));
    }
    
    @Transactional
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
    
    private void mapRequestToCoupon(CouponRequest request, Coupon coupon) {
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setType(Coupon.CouponType.valueOf(request.getType().toUpperCase()));
        coupon.setValue(request.getValue());
        coupon.setMinOrder(request.getMinOrder());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
    }
    
    private CouponDto toCouponDto(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setType(coupon.getType().name());
        dto.setValue(coupon.getValue());
        dto.setMinOrder(coupon.getMinOrder());
        dto.setMaxDiscount(coupon.getMaxDiscount());
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setUsedCount(coupon.getUsedCount());
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidUntil(coupon.getValidUntil());
        dto.setIsActive(coupon.getIsActive());
        return dto;
    }
}
