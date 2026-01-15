package com.sara.ecom.controller;

import com.sara.ecom.dto.AuthResponse;
import com.sara.ecom.dto.OtpRequest;
import com.sara.ecom.dto.OtpVerifyRequest;
import com.sara.ecom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }
    
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/oauth2/success")
    public ResponseEntity<AuthResponse> oauth2Success(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        // This endpoint will be called after OAuth2 success
        // In a real implementation, you'd extract this from the OAuth2 authentication object
        AuthResponse response = authService.handleOAuthLogin(email, providerId, firstName, lastName);
        return ResponseEntity.ok(response);
    }
}
