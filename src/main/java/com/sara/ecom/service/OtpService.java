package com.sara.ecom.service;

import com.sara.ecom.entity.OtpVerification;
import com.sara.ecom.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {
    
    private final OtpVerificationRepository otpVerificationRepository;
    
    @Value("${otp.length:6}")
    private int otpLength;
    
    @Value("${otp.expiration:300000}")
    private long otpExpirationMillis;
    
    private static final SecureRandom random = new SecureRandom();
    
    @Transactional
    public String generateOtp(String email) {
        // Delete existing OTP for this email
        otpVerificationRepository.deleteByEmail(email);
        
        // Generate new OTP
        String otp = generateRandomOtp();
        
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationMillis / 1000))
                .verified(false)
                .build();
        
        otpVerificationRepository.save(otpVerification);
        
        return otp;
    }
    
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<OtpVerification> otpVerificationOpt = 
            otpVerificationRepository.findByEmailAndOtpAndVerifiedFalse(email, otp);
        
        if (otpVerificationOpt.isEmpty()) {
            return false;
        }
        
        OtpVerification otpVerification = otpVerificationOpt.get();
        
        // Check if OTP is expired
        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpVerificationRepository.delete(otpVerification);
            return false;
        }
        
        // Mark as verified
        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);
        
        return true;
    }
    
    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    @Transactional
    public void deleteOtp(String email) {
        otpVerificationRepository.deleteByEmail(email);
    }
}
