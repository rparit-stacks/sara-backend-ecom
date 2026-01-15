package com.sara.ecom.controller;

import com.sara.ecom.dto.ShippingRuleDto;
import com.sara.ecom.dto.ShippingRuleRequest;
import com.sara.ecom.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShippingController {
    
    @Autowired
    private ShippingService shippingService;
    
    // Public endpoint for calculating shipping
    @PostMapping("/shipping/calculate")
    public ResponseEntity<Map<String, Object>> calculateShipping(@RequestBody Map<String, Object> request) {
        BigDecimal cartValue = new BigDecimal(request.get("cartValue").toString());
        String state = request.get("state") != null ? (String) request.get("state") : null;
        
        BigDecimal shipping = shippingService.calculateShipping(cartValue, state);
        
        return ResponseEntity.ok(Map.of(
            "shipping", shipping,
            "cartValue", cartValue,
            "state", state != null ? state : "N/A"
        ));
    }
    
    // Admin endpoints
    @GetMapping("/admin/shipping-rules")
    public ResponseEntity<List<ShippingRuleDto>> getAllShippingRules() {
        return ResponseEntity.ok(shippingService.getAllShippingRules());
    }
    
    @GetMapping("/admin/shipping-rules/{id}")
    public ResponseEntity<ShippingRuleDto> getShippingRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShippingRuleById(id));
    }
    
    @PostMapping("/admin/shipping-rules")
    public ResponseEntity<ShippingRuleDto> createShippingRule(@RequestBody ShippingRuleRequest request) {
        return ResponseEntity.ok(shippingService.createShippingRule(request));
    }
    
    @PutMapping("/admin/shipping-rules/{id}")
    public ResponseEntity<ShippingRuleDto> updateShippingRule(
            @PathVariable Long id,
            @RequestBody ShippingRuleRequest request) {
        return ResponseEntity.ok(shippingService.updateShippingRule(id, request));
    }
    
    @DeleteMapping("/admin/shipping-rules/{id}")
    public ResponseEntity<Void> deleteShippingRule(@PathVariable Long id) {
        shippingService.deleteShippingRule(id);
        return ResponseEntity.noContent().build();
    }
}
