package com.sara.ecom.controller;

import com.sara.ecom.dto.PaymentConfigDto;
import com.sara.ecom.service.PaymentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payment-config")
public class AdminPaymentConfigController {
    
    @Autowired
    private PaymentConfigService paymentConfigService;
    
    @GetMapping
    public ResponseEntity<PaymentConfigDto> getConfig() {
        return ResponseEntity.ok(paymentConfigService.getConfig());
    }
    
    @GetMapping("/with-secrets")
    public ResponseEntity<PaymentConfigDto> getConfigWithSecrets() {
        return ResponseEntity.ok(paymentConfigService.getConfigWithSecrets());
    }
    
    @PutMapping
    public ResponseEntity<PaymentConfigDto> updateConfig(@RequestBody PaymentConfigDto dto) {
        return ResponseEntity.ok(paymentConfigService.updateConfig(dto));
    }
}
