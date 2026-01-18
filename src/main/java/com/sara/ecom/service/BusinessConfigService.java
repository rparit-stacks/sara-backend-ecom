package com.sara.ecom.service;

import com.sara.ecom.dto.BusinessConfigDto;
import com.sara.ecom.entity.BusinessConfig;
import com.sara.ecom.repository.BusinessConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessConfigService {
    
    @Autowired
    private BusinessConfigRepository businessConfigRepository;
    
    public BusinessConfigDto getConfig() {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
        
        BusinessConfigDto dto = new BusinessConfigDto();
        dto.setId(config.getId());
        dto.setBusinessGstin(config.getBusinessGstin());
        dto.setBusinessName(config.getBusinessName());
        dto.setBusinessAddress(config.getBusinessAddress());
        dto.setBusinessState(config.getBusinessState());
        dto.setBusinessCity(config.getBusinessCity());
        dto.setBusinessPincode(config.getBusinessPincode());
        dto.setBusinessPhone(config.getBusinessPhone());
        dto.setBusinessEmail(config.getBusinessEmail());
        // Don't expose API keys in GET - return masked or empty
        dto.setSwipeApiKey(null);
        dto.setSwipeEnabled(config.getSwipeEnabled());
        dto.setEinvoiceEnabled(config.getEinvoiceEnabled());
        
        // Payment Gateway fields
        dto.setRazorpayKeyId(config.getRazorpayKeyId());
        dto.setRazorpayKeySecret(null); // Don't expose secret
        dto.setRazorpayEnabled(config.getRazorpayEnabled());
        dto.setStripePublicKey(config.getStripePublicKey());
        dto.setStripeSecretKey(null); // Don't expose secret
        dto.setStripeEnabled(config.getStripeEnabled());
        
        // Currency API fields
        dto.setCurrencyApiKey(null); // Don't expose API key
        dto.setCurrencyApiProvider(config.getCurrencyApiProvider());
        
        return dto;
    }
    
    public BusinessConfigDto getConfigWithApiKey() {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
        
        BusinessConfigDto dto = new BusinessConfigDto();
        dto.setId(config.getId());
        dto.setBusinessGstin(config.getBusinessGstin());
        dto.setBusinessName(config.getBusinessName());
        dto.setBusinessAddress(config.getBusinessAddress());
        dto.setBusinessState(config.getBusinessState());
        dto.setBusinessCity(config.getBusinessCity());
        dto.setBusinessPincode(config.getBusinessPincode());
        dto.setBusinessPhone(config.getBusinessPhone());
        dto.setBusinessEmail(config.getBusinessEmail());
        dto.setSwipeApiKey(config.getSwipeApiKey());
        dto.setSwipeEnabled(config.getSwipeEnabled());
        dto.setEinvoiceEnabled(config.getEinvoiceEnabled());
        
        // Payment Gateway fields
        dto.setRazorpayKeyId(config.getRazorpayKeyId());
        dto.setRazorpayKeySecret(config.getRazorpayKeySecret());
        dto.setRazorpayEnabled(config.getRazorpayEnabled());
        dto.setStripePublicKey(config.getStripePublicKey());
        dto.setStripeSecretKey(config.getStripeSecretKey());
        dto.setStripeEnabled(config.getStripeEnabled());
        
        // Currency API fields
        dto.setCurrencyApiKey(config.getCurrencyApiKey());
        dto.setCurrencyApiProvider(config.getCurrencyApiProvider());
        
        return dto;
    }
    
    @Transactional
    public BusinessConfigDto saveConfig(BusinessConfigDto dto) {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
        
        config.setBusinessGstin(dto.getBusinessGstin());
        config.setBusinessName(dto.getBusinessName());
        config.setBusinessAddress(dto.getBusinessAddress());
        config.setBusinessState(dto.getBusinessState());
        config.setBusinessCity(dto.getBusinessCity());
        config.setBusinessPincode(dto.getBusinessPincode());
        config.setBusinessPhone(dto.getBusinessPhone());
        config.setBusinessEmail(dto.getBusinessEmail());
        
        // Only update API key if provided (not empty/null)
        if (dto.getSwipeApiKey() != null && !dto.getSwipeApiKey().trim().isEmpty()) {
            config.setSwipeApiKey(dto.getSwipeApiKey());
        }
        
        config.setSwipeEnabled(dto.getSwipeEnabled() != null ? dto.getSwipeEnabled() : false);
        config.setEinvoiceEnabled(dto.getEinvoiceEnabled() != null ? dto.getEinvoiceEnabled() : false);
        
        // Payment Gateway fields
        if (dto.getRazorpayKeyId() != null && !dto.getRazorpayKeyId().trim().isEmpty()) {
            config.setRazorpayKeyId(dto.getRazorpayKeyId());
        }
        if (dto.getRazorpayKeySecret() != null && !dto.getRazorpayKeySecret().trim().isEmpty()) {
            config.setRazorpayKeySecret(dto.getRazorpayKeySecret());
        }
        config.setRazorpayEnabled(dto.getRazorpayEnabled() != null ? dto.getRazorpayEnabled() : false);
        
        if (dto.getStripePublicKey() != null && !dto.getStripePublicKey().trim().isEmpty()) {
            config.setStripePublicKey(dto.getStripePublicKey());
        }
        if (dto.getStripeSecretKey() != null && !dto.getStripeSecretKey().trim().isEmpty()) {
            config.setStripeSecretKey(dto.getStripeSecretKey());
        }
        config.setStripeEnabled(dto.getStripeEnabled() != null ? dto.getStripeEnabled() : false);
        
        // Currency API fields
        if (dto.getCurrencyApiKey() != null && !dto.getCurrencyApiKey().trim().isEmpty()) {
            config.setCurrencyApiKey(dto.getCurrencyApiKey());
        }
        if (dto.getCurrencyApiProvider() != null && !dto.getCurrencyApiProvider().trim().isEmpty()) {
            config.setCurrencyApiProvider(dto.getCurrencyApiProvider());
        }
        
        BusinessConfig saved = businessConfigRepository.save(config);
        
        BusinessConfigDto response = new BusinessConfigDto();
        response.setId(saved.getId());
        response.setBusinessGstin(saved.getBusinessGstin());
        response.setBusinessName(saved.getBusinessName());
        response.setBusinessAddress(saved.getBusinessAddress());
        response.setBusinessState(saved.getBusinessState());
        response.setBusinessCity(saved.getBusinessCity());
        response.setBusinessPincode(saved.getBusinessPincode());
        response.setBusinessPhone(saved.getBusinessPhone());
        response.setBusinessEmail(saved.getBusinessEmail());
        response.setSwipeApiKey(null); // Don't return API key
        response.setSwipeEnabled(saved.getSwipeEnabled());
        response.setEinvoiceEnabled(saved.getEinvoiceEnabled());
        
        // Payment Gateway fields
        response.setRazorpayKeyId(saved.getRazorpayKeyId());
        response.setRazorpayKeySecret(null); // Don't return secret
        response.setRazorpayEnabled(saved.getRazorpayEnabled());
        response.setStripePublicKey(saved.getStripePublicKey());
        response.setStripeSecretKey(null); // Don't return secret
        response.setStripeEnabled(saved.getStripeEnabled());
        
        // Currency API fields
        response.setCurrencyApiKey(null); // Don't return API key
        response.setCurrencyApiProvider(saved.getCurrencyApiProvider());
        
        return response;
    }
    
    public boolean isSwipeEnabled() {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
        return config.getSwipeEnabled() != null && config.getSwipeEnabled();
    }
    
    public String getApiKey() {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
        return config.getSwipeApiKey();
    }
    
    public BusinessConfig getConfigEntity() {
        return businessConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new BusinessConfig());
    }
}
