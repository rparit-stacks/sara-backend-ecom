package com.sara.ecom.controller;

import com.sara.ecom.dto.BusinessConfigDto;
import com.sara.ecom.service.BusinessConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/business-config")
public class AdminBusinessConfigController {
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    @GetMapping
    public ResponseEntity<BusinessConfigDto> getConfig() {
        return ResponseEntity.ok(businessConfigService.getConfig());
    }
    
    @PutMapping
    public ResponseEntity<BusinessConfigDto> updateConfig(@RequestBody BusinessConfigDto dto) {
        return ResponseEntity.ok(businessConfigService.saveConfig(dto));
    }
}
