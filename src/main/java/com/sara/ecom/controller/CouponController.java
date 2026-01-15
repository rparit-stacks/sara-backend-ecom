package com.sara.ecom.controller;

import com.sara.ecom.dto.CouponDto;
import com.sara.ecom.dto.CouponRequest;
import com.sara.ecom.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CouponController {
    
    @Autowired
    private CouponService couponService;
    
    // Public endpoint for validating coupons
    @PostMapping("/coupons/validate")
    public ResponseEntity<CouponDto> validateCoupon(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        BigDecimal orderTotal = new BigDecimal(request.get("orderTotal").toString());
        return ResponseEntity.ok(couponService.validateCoupon(code, orderTotal));
    }
    
    // Admin endpoints
    @GetMapping("/admin/coupons")
    public ResponseEntity<List<CouponDto>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }
    
    @GetMapping("/admin/coupons/{id}")
    public ResponseEntity<CouponDto> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCouponById(id));
    }
    
    @PostMapping("/admin/coupons")
    public ResponseEntity<CouponDto> createCoupon(@RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }
    
    @PutMapping("/admin/coupons/{id}")
    public ResponseEntity<CouponDto> updateCoupon(
            @PathVariable Long id,
            @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }
    
    @DeleteMapping("/admin/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
