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
        // Don't expose API keys in GET - return placeholder if key exists
        if (config.getSwipeApiKey() != null && !config.getSwipeApiKey().trim().isEmpty()) {
            dto.setSwipeApiKey("***API_KEY_SET***"); // Placeholder to indicate key exists
        } else {
        dto.setSwipeApiKey(null);
        }
        dto.setSwipeEnabled(config.getSwipeEnabled());
        dto.setEinvoiceEnabled(config.getEinvoiceEnabled());
        
        // Currency API fields
        dto.setCurrencyApiKey(null); // Don't expose API key
        dto.setCurrencyApiProvider(config.getCurrencyApiProvider());
        
        // DoubleTick WhatsApp fields
        if (config.getDoubletickApiKey() != null && !config.getDoubletickApiKey().trim().isEmpty()) {
            dto.setDoubletickApiKey("***API_KEY_SET***"); // Placeholder to indicate key exists
        } else {
            dto.setDoubletickApiKey(null);
        }
        dto.setDoubletickSenderNumber(config.getDoubletickSenderNumber());
        dto.setDoubletickTemplateName(config.getDoubletickTemplateName());
        dto.setDoubletickEnabled(config.getDoubletickEnabled());
        
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
        
        // Currency API fields
        dto.setCurrencyApiKey(config.getCurrencyApiKey());
        dto.setCurrencyApiProvider(config.getCurrencyApiProvider());
        
        // DoubleTick WhatsApp fields
        dto.setDoubletickApiKey(config.getDoubletickApiKey());
        dto.setDoubletickSenderNumber(config.getDoubletickSenderNumber());
        dto.setDoubletickTemplateName(config.getDoubletickTemplateName());
        dto.setDoubletickEnabled(config.getDoubletickEnabled());
        
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
        
        // Only update API key if provided (not empty/null and not the placeholder)
        if (dto.getSwipeApiKey() != null && !dto.getSwipeApiKey().trim().isEmpty() 
            && !dto.getSwipeApiKey().equals("***API_KEY_SET***")) {
            config.setSwipeApiKey(dto.getSwipeApiKey().trim());
        }
        
        config.setSwipeEnabled(dto.getSwipeEnabled() != null ? dto.getSwipeEnabled() : false);
        config.setEinvoiceEnabled(dto.getEinvoiceEnabled() != null ? dto.getEinvoiceEnabled() : false);
        
        // Currency API fields
        if (dto.getCurrencyApiKey() != null && !dto.getCurrencyApiKey().trim().isEmpty()) {
            config.setCurrencyApiKey(dto.getCurrencyApiKey());
        }
        if (dto.getCurrencyApiProvider() != null && !dto.getCurrencyApiProvider().trim().isEmpty()) {
            config.setCurrencyApiProvider(dto.getCurrencyApiProvider());
        }
        
        // DoubleTick WhatsApp fields
        // Only update API key if provided (not empty/null and not the placeholder/masked)
        if (dto.getDoubletickApiKey() != null && !dto.getDoubletickApiKey().trim().isEmpty() 
            && !dto.getDoubletickApiKey().equals("***API_KEY_SET***")
            && !dto.getDoubletickApiKey().startsWith("***")) {
            // It's a new full API key, update it
            config.setDoubletickApiKey(dto.getDoubletickApiKey().trim());
        }
        // If null, empty, masked, or placeholder - don't update, preserve existing key from DB
        
        if (dto.getDoubletickSenderNumber() != null && !dto.getDoubletickSenderNumber().trim().isEmpty()) {
            config.setDoubletickSenderNumber(dto.getDoubletickSenderNumber().trim());
        }
        if (dto.getDoubletickEnabled() != null) {
            config.setDoubletickEnabled(dto.getDoubletickEnabled());
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
        
        // Currency API fields
        response.setCurrencyApiKey(null); // Don't return API key
        response.setCurrencyApiProvider(saved.getCurrencyApiProvider());
        
        // DoubleTick WhatsApp fields
        response.setDoubletickApiKey(null); // Don't return API key
        response.setDoubletickSenderNumber(saved.getDoubletickSenderNumber());
        response.setDoubletickTemplateName(saved.getDoubletickTemplateName());
        response.setDoubletickEnabled(saved.getDoubletickEnabled());
        
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
