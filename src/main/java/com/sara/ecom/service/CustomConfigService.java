package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.CustomConfigDto;
import com.sara.ecom.dto.CustomConfigRequest;
import com.sara.ecom.dto.CustomDesignRequestDto;
import com.sara.ecom.entity.CustomDesignRequest;
import com.sara.ecom.entity.CustomFormField;
import com.sara.ecom.entity.CustomProductConfig;
import com.sara.ecom.repository.CustomDesignRequestRepository;
import com.sara.ecom.repository.CustomFormFieldRepository;
import com.sara.ecom.repository.CustomProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomConfigService {
    
    @Autowired
    private CustomProductConfigRepository configRepository;
    
    @Autowired
    private CustomFormFieldRepository formFieldRepository;
    
    @Autowired
    private CustomDesignRequestRepository designRequestRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Get config for public use
    public CustomConfigDto getPublicConfig() {
        CustomProductConfig config = configRepository.findAll().stream()
                .findFirst()
                .orElse(getDefaultConfig());
        
        CustomConfigDto dto = toConfigDto(config);
        dto.setFormFields(getAllFormFields());
        return dto;
    }
    
    // Get config for admin
    public CustomConfigDto getAdminConfig() {
        return getPublicConfig();
    }
    
    // Update config
    @Transactional
    public CustomConfigDto updateConfig(CustomConfigRequest request) {
        CustomProductConfig config = configRepository.findAll().stream()
                .findFirst()
                .orElse(new CustomProductConfig());
        
        config.setPageTitle(request.getPageTitle());
        config.setPageDescription(request.getPageDescription());
        config.setUploadLabel(request.getUploadLabel());
        config.setDesignPrice(request.getDesignPrice());
        config.setMinQuantity(request.getMinQuantity());
        config.setMaxQuantity(request.getMaxQuantity());
        config.setTermsAndConditions(request.getTermsAndConditions());
        
        configRepository.save(config);
        
        CustomConfigDto dto = toConfigDto(config);
        dto.setFormFields(getAllFormFields());
        return dto;
    }
    
    // Form Field management
    public List<CustomConfigDto.FormFieldDto> getAllFormFields() {
        return formFieldRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toFormFieldDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CustomConfigDto.FormFieldDto createFormField(CustomConfigRequest.FormFieldRequest request) {
        CustomFormField field = new CustomFormField();
        mapFormFieldRequest(request, field);
        return toFormFieldDto(formFieldRepository.save(field));
    }
    
    @Transactional
    public CustomConfigDto.FormFieldDto updateFormField(Long id, CustomConfigRequest.FormFieldRequest request) {
        CustomFormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form field not found"));
        mapFormFieldRequest(request, field);
        return toFormFieldDto(formFieldRepository.save(field));
    }
    
    @Transactional
    public void deleteFormField(Long id) {
        formFieldRepository.deleteById(id);
    }
    
    // Custom Design Requests
    public List<CustomDesignRequestDto> getAllDesignRequests() {
        return designRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDesignRequestDto)
                .collect(Collectors.toList());
    }
    
    public CustomDesignRequestDto getDesignRequestById(Long id) {
        CustomDesignRequest request = designRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Design request not found"));
        return toDesignRequestDto(request);
    }
    
    @Transactional
    public CustomDesignRequestDto submitDesignRequest(CustomDesignRequestDto request) {
        CustomDesignRequest entity = new CustomDesignRequest();
        entity.setFullName(request.getFullName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setDesignType(request.getDesignType());
        entity.setDescription(request.getDescription());
        entity.setReferenceImage(request.getReferenceImage());
        return toDesignRequestDto(designRequestRepository.save(entity));
    }
    
    @Transactional
    public CustomDesignRequestDto updateDesignRequestStatus(Long id, String status, String adminNotes) {
        CustomDesignRequest request = designRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Design request not found"));
        request.setStatus(CustomDesignRequest.Status.valueOf(status.toUpperCase()));
        if (adminNotes != null) {
            request.setAdminNotes(adminNotes);
        }
        return toDesignRequestDto(designRequestRepository.save(request));
    }
    
    // Helper methods
    private CustomProductConfig getDefaultConfig() {
        CustomProductConfig config = new CustomProductConfig();
        config.setPageTitle("Make Your Own");
        config.setPageDescription("Create your custom design");
        config.setUploadLabel("Upload your design");
        return config;
    }
    
    private void mapFormFieldRequest(CustomConfigRequest.FormFieldRequest request, CustomFormField field) {
        field.setType(request.getType());
        field.setLabel(request.getLabel());
        field.setPlaceholder(request.getPlaceholder());
        field.setRequired(request.getRequired() != null ? request.getRequired() : false);
        field.setMinValue(request.getMinValue());
        field.setMaxValue(request.getMaxValue());
        field.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            try {
                field.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            } catch (JsonProcessingException e) {
                field.setOptions("[]");
            }
        }
    }
    
    private CustomConfigDto toConfigDto(CustomProductConfig config) {
        CustomConfigDto dto = new CustomConfigDto();
        dto.setId(config.getId());
        dto.setPageTitle(config.getPageTitle());
        dto.setPageDescription(config.getPageDescription());
        dto.setUploadLabel(config.getUploadLabel());
        dto.setDesignPrice(config.getDesignPrice());
        dto.setMinQuantity(config.getMinQuantity());
        dto.setMaxQuantity(config.getMaxQuantity());
        dto.setTermsAndConditions(config.getTermsAndConditions());
        return dto;
    }
    
    private CustomConfigDto.FormFieldDto toFormFieldDto(CustomFormField field) {
        CustomConfigDto.FormFieldDto dto = new CustomConfigDto.FormFieldDto();
        dto.setId(field.getId());
        dto.setType(field.getType());
        dto.setLabel(field.getLabel());
        dto.setPlaceholder(field.getPlaceholder());
        dto.setRequired(field.getRequired());
        dto.setMinValue(field.getMinValue());
        dto.setMaxValue(field.getMaxValue());
        dto.setDisplayOrder(field.getDisplayOrder());
        
        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            try {
                dto.setOptions(objectMapper.readValue(field.getOptions(), new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                dto.setOptions(new ArrayList<>());
            }
        }
        
        return dto;
    }
    
    private CustomDesignRequestDto toDesignRequestDto(CustomDesignRequest request) {
        CustomDesignRequestDto dto = new CustomDesignRequestDto();
        dto.setId(request.getId());
        dto.setFullName(request.getFullName());
        dto.setEmail(request.getEmail());
        dto.setPhone(request.getPhone());
        dto.setDesignType(request.getDesignType());
        dto.setDescription(request.getDescription());
        dto.setReferenceImage(request.getReferenceImage());
        dto.setStatus(request.getStatus().name());
        dto.setAdminNotes(request.getAdminNotes());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }
}
