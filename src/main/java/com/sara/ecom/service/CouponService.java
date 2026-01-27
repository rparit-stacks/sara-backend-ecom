package com.sara.ecom.service;

import com.sara.ecom.dto.CouponDto;
import com.sara.ecom.dto.CouponRequest;
import com.sara.ecom.entity.Coupon;
import com.sara.ecom.entity.CouponUsage;
import com.sara.ecom.repository.CouponRepository;
import com.sara.ecom.repository.CouponUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CouponService {
    
    @Autowired
    private CouponRepository couponRepository;
    
    @Autowired
    private CouponUsageRepository couponUsageRepository;
    
    public CouponDto validateCoupon(String code, BigDecimal orderTotal, String userEmail) {
        CouponDto response = new CouponDto();
        response.setCode(code);
        
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code).orElse(null);
        
        if (coupon == null) {
            response.setValid(false);
            response.setMessage("Invalid coupon code");
            return response;
        }
        
        // User-specific coupons: only the allowed user can use them
        if (coupon.getApplicability() == Coupon.Applicability.USER_SPECIFIC) {
            if (userEmail == null || userEmail.isBlank()) {
                response.setValid(false);
                response.setMessage("Login required to use this coupon");
                return response;
            }
            if (coupon.getAllowedUserEmail() == null || !userEmail.trim().equalsIgnoreCase(coupon.getAllowedUserEmail().trim())) {
                response.setValid(false);
                response.setMessage("This coupon is not valid for your account");
                return response;
            }
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
        
        // Check per-user usage limit
        if (userEmail != null && coupon.getPerUserUsageLimit() != null) {
            Optional<CouponUsage> usage = couponUsageRepository.findByCouponAndUserEmail(coupon, userEmail);
            if (usage.isPresent() && usage.get().getUsageCount() >= coupon.getPerUserUsageLimit()) {
                response.setValid(false);
                response.setMessage("You have reached the maximum usage limit for this coupon");
                return response;
            }
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
    public void useCoupon(String code, String userEmail) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        
        // Increment global usage count
        coupon.incrementUsedCount();
        couponRepository.save(coupon);
        
        // Track per-user usage
        if (userEmail != null) {
            CouponUsage usage = couponUsageRepository.findByCouponAndUserEmail(coupon, userEmail)
                    .orElseGet(() -> {
                        CouponUsage newUsage = new CouponUsage();
                        newUsage.setCoupon(coupon);
                        newUsage.setUserEmail(userEmail);
                        return newUsage;
                    });
            usage.incrementUsage();
            couponUsageRepository.save(usage);
        }
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
        coupon.setPerUserUsageLimit(request.getPerUserUsageLimit());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        if (request.getApplicability() != null && !request.getApplicability().isBlank()) {
            coupon.setApplicability(Coupon.Applicability.valueOf(request.getApplicability().toUpperCase()));
        } else {
            coupon.setApplicability(Coupon.Applicability.GLOBAL);
        }
        coupon.setAllowedUserEmail(request.getAllowedUserEmail() != null && !request.getAllowedUserEmail().isBlank() ? request.getAllowedUserEmail().trim() : null);
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
        dto.setPerUserUsageLimit(coupon.getPerUserUsageLimit());
        dto.setUsedCount(coupon.getUsedCount());
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidUntil(coupon.getValidUntil());
        dto.setIsActive(coupon.getIsActive());
        dto.setApplicability(coupon.getApplicability() != null ? coupon.getApplicability().name() : "GLOBAL");
        dto.setAllowedUserEmail(coupon.getAllowedUserEmail());
        return dto;
    }
    
    /**
     * Returns coupons eligible for the given user and order total.
     * Requires userEmail (logged-in user). Returns only coupons that are valid now, active,
     * within usage limits, pass minOrder, and are either GLOBAL or USER_SPECIFIC for this user.
     */
    public List<CouponDto> getEligible(BigDecimal orderTotal, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .filter(c -> {
                    if (c.getApplicability() == Coupon.Applicability.USER_SPECIFIC) {
                        if (c.getAllowedUserEmail() == null || !userEmail.trim().equalsIgnoreCase(c.getAllowedUserEmail().trim())) {
                            return false;
                        }
                    }
                    if (c.getValidFrom() != null && now.isBefore(c.getValidFrom())) return false;
                    if (c.getValidUntil() != null && now.isAfter(c.getValidUntil())) return false;
                    if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) return false;
                    if (c.getPerUserUsageLimit() != null) {
                        Optional<CouponUsage> usage = couponUsageRepository.findByCouponAndUserEmail(c, userEmail);
                        if (usage.isPresent() && usage.get().getUsageCount() >= c.getPerUserUsageLimit()) {
                            return false;
                        }
                    }
                    if (c.getMinOrder() != null && orderTotal.compareTo(c.getMinOrder()) < 0) return false;
                    return true;
                })
                .map(c -> {
                    CouponDto dto = toCouponDto(c);
                    dto.setValid(true);
                    BigDecimal discount;
                    if (c.getType() == Coupon.CouponType.PERCENTAGE) {
                        discount = orderTotal.multiply(c.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        if (c.getMaxDiscount() != null && discount.compareTo(c.getMaxDiscount()) > 0) {
                            discount = c.getMaxDiscount();
                        }
                        dto.setMessage(c.getValue() + "% off" + (c.getMaxDiscount() != null ? " (max ₹" + c.getMaxDiscount() + ")" : ""));
                    } else {
                        discount = c.getValue();
                        dto.setMessage("₹" + c.getValue() + " off");
                    }
                    dto.setDiscount(discount);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
