package com.sara.ecom.controller;

import com.sara.ecom.dto.CustomConfigDto;
import com.sara.ecom.dto.CustomConfigRequest;
import com.sara.ecom.dto.CustomDesignRequestDto;
import com.sara.ecom.service.CustomConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomConfigController {
    
    @Autowired
    private CustomConfigService customConfigService;
    
    // Public endpoints
    @GetMapping("/custom-config")
    public ResponseEntity<CustomConfigDto> getPublicConfig() {
        return ResponseEntity.ok(customConfigService.getPublicConfig());
    }
    
    @PostMapping("/custom-design-requests")
    public ResponseEntity<CustomDesignRequestDto> submitDesignRequest(@RequestBody CustomDesignRequestDto request) {
        return ResponseEntity.ok(customConfigService.submitDesignRequest(request));
    }
    
    // Admin endpoints
    @GetMapping("/admin/custom-config")
    public ResponseEntity<CustomConfigDto> getAdminConfig() {
        return ResponseEntity.ok(customConfigService.getAdminConfig());
    }
    
    @PutMapping("/admin/custom-config")
    public ResponseEntity<CustomConfigDto> updateConfig(@RequestBody CustomConfigRequest request) {
        return ResponseEntity.ok(customConfigService.updateConfig(request));
    }
    
    // Form fields
    @PostMapping("/admin/custom-config/fields")
    public ResponseEntity<CustomConfigDto.FormFieldDto> createFormField(
            @RequestBody CustomConfigRequest.FormFieldRequest request) {
        return ResponseEntity.ok(customConfigService.createFormField(request));
    }
    
    @PutMapping("/admin/custom-config/fields/{id}")
    public ResponseEntity<CustomConfigDto.FormFieldDto> updateFormField(
            @PathVariable Long id,
            @RequestBody CustomConfigRequest.FormFieldRequest request) {
        return ResponseEntity.ok(customConfigService.updateFormField(id, request));
    }
    
    @DeleteMapping("/admin/custom-config/fields/{id}")
    public ResponseEntity<Void> deleteFormField(@PathVariable Long id) {
        customConfigService.deleteFormField(id);
        return ResponseEntity.noContent().build();
    }
    
    // Design requests
    @GetMapping("/admin/custom-design-requests")
    public ResponseEntity<List<CustomDesignRequestDto>> getAllDesignRequests() {
        return ResponseEntity.ok(customConfigService.getAllDesignRequests());
    }
    
    @GetMapping("/admin/custom-design-requests/{id}")
    public ResponseEntity<CustomDesignRequestDto> getDesignRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(customConfigService.getDesignRequestById(id));
    }
    
    @PutMapping("/admin/custom-design-requests/{id}")
    public ResponseEntity<CustomDesignRequestDto> updateDesignRequestStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String adminNotes = request.get("adminNotes");
        return ResponseEntity.ok(customConfigService.updateDesignRequestStatus(id, status, adminNotes));
    }
}
