package com.sara.ecom.controller;

import com.sara.ecom.dto.AdminAuthResponse;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminLoginRequest;
import com.sara.ecom.dto.AdminSignupRequest;
import com.sara.ecom.service.AdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    
    @Autowired
    private AdminAuthService adminAuthService;
    
    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
    
    @PostMapping("/signup")
    public ResponseEntity<AdminAuthResponse> signup(@RequestBody AdminSignupRequest request) {
        return ResponseEntity.ok(adminAuthService.signup(request));
    }
    
    @GetMapping("/me")
    public ResponseEntity<AdminDto> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(adminAuthService.getCurrentAdmin(token));
    }
}
