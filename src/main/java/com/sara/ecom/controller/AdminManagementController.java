package com.sara.ecom.controller;

import com.sara.ecom.dto.AdminCreateRequest;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminUpdateRequest;
import com.sara.ecom.dto.AdminInviteRequest;
import com.sara.ecom.dto.AdminInviteAcceptRequest;
import com.sara.ecom.entity.AdminInvite;
import com.sara.ecom.service.AdminService;
import com.sara.ecom.service.JwtService;
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
    
    @Autowired
    private JwtService jwtService;
    
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
    
    /**
     * Send admin invitation email
     */
    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> sendInvite(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AdminInviteRequest request) {
        String invitedByEmail = getAdminEmailFromHeader(authHeader);
        adminService.sendAdminInvite(request.getEmail(), invitedByEmail);
        return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
    }
    
    /**
     * Get invite details by token (for invite page)
     */
    @GetMapping("/invite/{token}")
    public ResponseEntity<Map<String, Object>> getInviteByToken(@PathVariable String token) {
        AdminInvite invite = adminService.getInviteByToken(token);
        return ResponseEntity.ok(Map.of(
            "email", invite.getEmail(),
            "status", invite.getStatus().name(),
            "expiresAt", invite.getExpiresAt().toString(),
            "invitedBy", invite.getInvitedBy() != null ? invite.getInvitedBy() : ""
        ));
    }
    
    /**
     * Accept invitation and create admin account
     */
    @PostMapping("/invite/accept")
    public ResponseEntity<AdminDto> acceptInvite(@RequestBody AdminInviteAcceptRequest request) {
        AdminDto admin = adminService.acceptAdminInvite(request);
        return ResponseEntity.ok(admin);
    }
    
    private String getAdminEmailFromHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return "system";
        }
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            return jwtService.extractEmail(token);
        } catch (Exception e) {
            return "system";
        }
    }
}
