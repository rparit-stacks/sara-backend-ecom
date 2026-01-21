package com.sara.ecom.service;

import com.sara.ecom.dto.PaymentConfigDto;
import com.sara.ecom.entity.PaymentConfig;
import com.sara.ecom.repository.PaymentConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentConfigService {
    
    @Autowired
    private PaymentConfigRepository paymentConfigRepository;
    
    public PaymentConfigDto getConfig() {
        PaymentConfig config = paymentConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new PaymentConfig());
        
        PaymentConfigDto dto = new PaymentConfigDto();
        dto.setId(config.getId());
        
        // Razorpay fields
        dto.setRazorpayKeyId(config.getRazorpayKeyId());
        dto.setRazorpayKeySecret(null); // Mask secret in GET
        dto.setRazorpayEnabled(config.getRazorpayEnabled() != null ? config.getRazorpayEnabled() : false);
        
        // Stripe fields
        dto.setStripePublicKey(config.getStripePublicKey());
        dto.setStripeSecretKey(null); // Mask secret in GET
        dto.setStripeEnabled(config.getStripeEnabled() != null ? config.getStripeEnabled() : false);
        
        // COD fields
        dto.setCodEnabled(config.getCodEnabled() != null ? config.getCodEnabled() : false);
        dto.setPartialCodEnabled(config.getPartialCodEnabled() != null ? config.getPartialCodEnabled() : false);
        dto.setPartialCodAdvancePercentage(config.getPartialCodAdvancePercentage());
        
        return dto;
    }
    
    public PaymentConfigDto getConfigWithSecrets() {
        PaymentConfig config = paymentConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new PaymentConfig());
        
        PaymentConfigDto dto = new PaymentConfigDto();
        dto.setId(config.getId());
        
        // Razorpay fields (with secrets)
        dto.setRazorpayKeyId(config.getRazorpayKeyId());
        dto.setRazorpayKeySecret(config.getRazorpayKeySecret());
        dto.setRazorpayEnabled(config.getRazorpayEnabled() != null ? config.getRazorpayEnabled() : false);
        
        // Stripe fields (with secrets)
        dto.setStripePublicKey(config.getStripePublicKey());
        dto.setStripeSecretKey(config.getStripeSecretKey());
        dto.setStripeEnabled(config.getStripeEnabled() != null ? config.getStripeEnabled() : false);
        
        // COD fields
        dto.setCodEnabled(config.getCodEnabled() != null ? config.getCodEnabled() : false);
        dto.setPartialCodEnabled(config.getPartialCodEnabled() != null ? config.getPartialCodEnabled() : false);
        dto.setPartialCodAdvancePercentage(config.getPartialCodAdvancePercentage());
        
        return dto;
    }
    
    @Transactional
    public PaymentConfigDto updateConfig(PaymentConfigDto dto) {
        PaymentConfig config = paymentConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new PaymentConfig());
        
        // Update Razorpay fields (only if provided and not placeholder)
        if (dto.getRazorpayKeyId() != null && !dto.getRazorpayKeyId().trim().isEmpty() 
            && !dto.getRazorpayKeyId().equals("***API_KEY_SET***")) {
            config.setRazorpayKeyId(dto.getRazorpayKeyId().trim());
        }
        if (dto.getRazorpayKeySecret() != null && !dto.getRazorpayKeySecret().trim().isEmpty() 
            && !dto.getRazorpayKeySecret().equals("***API_KEY_SET***")) {
            config.setRazorpayKeySecret(dto.getRazorpayKeySecret().trim());
        }
        config.setRazorpayEnabled(dto.getRazorpayEnabled() != null ? dto.getRazorpayEnabled() : false);
        
        // Update Stripe fields (only if provided and not placeholder)
        if (dto.getStripePublicKey() != null && !dto.getStripePublicKey().trim().isEmpty() 
            && !dto.getStripePublicKey().equals("***API_KEY_SET***")) {
            config.setStripePublicKey(dto.getStripePublicKey().trim());
        }
        if (dto.getStripeSecretKey() != null && !dto.getStripeSecretKey().trim().isEmpty() 
            && !dto.getStripeSecretKey().equals("***API_KEY_SET***")) {
            config.setStripeSecretKey(dto.getStripeSecretKey().trim());
        }
        config.setStripeEnabled(dto.getStripeEnabled() != null ? dto.getStripeEnabled() : false);
        
        // Update COD fields with validation
        Boolean codEnabled = dto.getCodEnabled() != null ? dto.getCodEnabled() : false;
        Boolean partialCodEnabled = dto.getPartialCodEnabled() != null ? dto.getPartialCodEnabled() : false;
        
        // Validation Rule 1: COD & Partial COD Mutual Exclusivity
        if (partialCodEnabled && codEnabled) {
            // If both are true, prioritize Partial COD (auto-disable COD)
            codEnabled = false;
        }
        config.setCodEnabled(codEnabled);
        config.setPartialCodEnabled(partialCodEnabled);
        
        // Validation Rule 2: Partial COD Gateway Requirement
        if (partialCodEnabled) {
            Boolean razorpayEnabled = config.getRazorpayEnabled() != null ? config.getRazorpayEnabled() : false;
            Boolean stripeEnabled = config.getStripeEnabled() != null ? config.getStripeEnabled() : false;
            
            if (!razorpayEnabled && !stripeEnabled) {
                throw new IllegalArgumentException("Partial COD requires at least one online payment gateway (Razorpay or Stripe) to be enabled");
            }
            
            // Validation Rule 3: Partial COD Advance Percentage
            if (dto.getPartialCodAdvancePercentage() == null || 
                dto.getPartialCodAdvancePercentage() < 10 || 
                dto.getPartialCodAdvancePercentage() > 90) {
                throw new IllegalArgumentException("Partial COD advance percentage must be between 10 and 90");
            }
            config.setPartialCodAdvancePercentage(dto.getPartialCodAdvancePercentage());
        } else {
            config.setPartialCodAdvancePercentage(null);
        }
        
        PaymentConfig saved = paymentConfigRepository.save(config);
        
        // Return DTO with masked secrets
        PaymentConfigDto response = new PaymentConfigDto();
        response.setId(saved.getId());
        response.setRazorpayKeyId(saved.getRazorpayKeyId());
        response.setRazorpayKeySecret(null); // Mask secret
        response.setRazorpayEnabled(saved.getRazorpayEnabled());
        response.setStripePublicKey(saved.getStripePublicKey());
        response.setStripeSecretKey(null); // Mask secret
        response.setStripeEnabled(saved.getStripeEnabled());
        response.setCodEnabled(saved.getCodEnabled());
        response.setPartialCodEnabled(saved.getPartialCodEnabled());
        response.setPartialCodAdvancePercentage(saved.getPartialCodAdvancePercentage());
        
        return response;
    }
    
    public PaymentConfig getConfigEntity() {
        return paymentConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new PaymentConfig());
    }
}
