package com.sara.ecom.controller;

import com.sara.ecom.dto.AdminCreateRequest;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminUpdateRequest;
import com.sara.ecom.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/admins")
public class AdminManagementController {
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AdminDto> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }
    
    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminCreateRequest request) {
        return ResponseEntity.ok(adminService.createAdmin(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AdminDto> updateAdmin(
            @PathVariable Long id,
            @RequestBody AdminUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateAdmin(id, request));
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<AdminDto> updateAdminStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(adminService.updateAdminStatus(id, status));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok().build();
    }
}
