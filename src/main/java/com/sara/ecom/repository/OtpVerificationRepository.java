package com.sara.ecom.repository;

import com.sara.ecom.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByEmailAndOtpAndVerifiedFalse(String email, String otp);
    Optional<OtpVerification> findByEmailAndVerifiedFalse(String email);
    void deleteByEmail(String email);
}
