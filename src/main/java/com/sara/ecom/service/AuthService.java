package com.sara.ecom.service;

import com.sara.ecom.dto.AuthResponse;
import com.sara.ecom.dto.OtpRequest;
import com.sara.ecom.dto.OtpVerifyRequest;
import com.sara.ecom.entity.User;
import com.sara.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    
    @Transactional
    public void requestOtp(OtpRequest request) {
        String otp = otpService.generateOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);
    }
    
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        // Find or create user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .authProvider(User.AuthProvider.OTP)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });
        
        // Update email verified status
        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        
        // Generate JWT token using email as subject
        String token = jwtService.generateToken(user.getEmail());
        
        // Delete OTP after successful verification
        otpService.deleteOtp(request.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }
    
    @Transactional
    public AuthResponse handleOAuthLogin(String email, String oauthProviderId, String firstName, String lastName) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .authProvider(User.AuthProvider.GOOGLE)
                            .oauthProviderId(oauthProviderId)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });
        
        // Update OAuth info if needed
        if (user.getOauthProviderId() == null || !user.getOauthProviderId().equals(oauthProviderId)) {
            user.setOauthProviderId(oauthProviderId);
            user.setAuthProvider(User.AuthProvider.GOOGLE);
        }
        
        // Update name if provided and not set
        if (firstName != null && user.getFirstName() == null) {
            user.setFirstName(firstName);
        }
        if (lastName != null && user.getLastName() == null) {
            user.setLastName(lastName);
        }
        
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);
        
        String token = jwtService.generateToken(savedUser.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .message("OAuth login successful")
                .build();
    }
}
