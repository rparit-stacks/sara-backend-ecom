package com.sara.ecom.service;

import com.sara.ecom.dto.AuthResponse;
import com.sara.ecom.dto.EmailTemplateData;
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
        // Convert email to lowercase to prevent duplicate accounts
        String email = request.getEmail().toLowerCase().trim();
        String otp = otpService.generateOtp(email);
        emailService.sendOtpEmail(email, otp);
    }
    
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        // Convert email to lowercase to prevent duplicate accounts
        String email = request.getEmail().toLowerCase().trim();
        boolean isValid = otpService.verifyOtp(email, request.getOtp());
        
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        // Find or create user
        boolean isNewUser = !userRepository.existsByEmail(email);
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
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
        
        // Send welcome email if this is a new user
        if (isNewUser) {
            try {
                String recipientName = (user.getFirstName() != null ? user.getFirstName() : "") + 
                                     (user.getLastName() != null ? " " + user.getLastName() : "");
                if (recipientName.trim().isEmpty()) {
                    recipientName = user.getEmail();
                }
                
                EmailTemplateData.WelcomeEmailData emailData = new EmailTemplateData.WelcomeEmailData();
                emailData.setRecipientName(recipientName.trim());
                emailData.setRecipientEmail(user.getEmail());
                
                emailService.sendWelcomeEmail(emailData);
            } catch (Exception e) {
                // Log error but don't fail login
                System.err.println("Failed to send welcome email: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Generate JWT token using email as subject
        String token = jwtService.generateToken(user.getEmail());
        
        // Delete OTP after successful verification
        otpService.deleteOtp(email);
        
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }
    
    @Transactional
    public AuthResponse handleOAuthLogin(String email, String oauthProviderId, String firstName, String lastName) {
        // Convert email to lowercase to prevent duplicate accounts
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        boolean isNewUser = normalizedEmail != null && !userRepository.existsByEmail(normalizedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(normalizedEmail)
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
        
        // Send welcome email if this is a new user
        if (isNewUser) {
            try {
                String recipientName = (savedUser.getFirstName() != null ? savedUser.getFirstName() : "") + 
                                     (savedUser.getLastName() != null ? " " + savedUser.getLastName() : "");
                if (recipientName.trim().isEmpty()) {
                    recipientName = savedUser.getEmail();
                }
                
                EmailTemplateData.WelcomeEmailData emailData = new EmailTemplateData.WelcomeEmailData();
                emailData.setRecipientName(recipientName.trim());
                emailData.setRecipientEmail(savedUser.getEmail());
                
                emailService.sendWelcomeEmail(emailData);
            } catch (Exception e) {
                // Log error but don't fail login
                System.err.println("Failed to send welcome email: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        String token = jwtService.generateToken(savedUser.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .message("OAuth login successful")
                .build();
    }
}
