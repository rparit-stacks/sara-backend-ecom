package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.CustomConfigDto;
import com.sara.ecom.dto.CustomConfigRequest;
import com.sara.ecom.dto.CustomDesignRequestDto;
import com.sara.ecom.dto.EmailTemplateData;
import com.sara.ecom.entity.CustomDesignRequest;
import com.sara.ecom.entity.CustomFormField;
import com.sara.ecom.entity.CustomProductConfig;
import com.sara.ecom.repository.CustomDesignRequestRepository;
import com.sara.ecom.repository.CustomFormFieldRepository;
import com.sara.ecom.repository.CustomProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private com.sara.ecom.repository.CustomConfigVariantRepository variantRepository;
    
    @Autowired
    private com.sara.ecom.repository.CustomConfigPricingSlabRepository pricingSlabRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Get config for public use
    public CustomConfigDto getPublicConfig() {
        CustomProductConfig config = configRepository.findAll().stream()
                .findFirst()
                .orElse(getDefaultConfig());
        
        CustomConfigDto dto = toConfigDto(config);
        dto.setFormFields(getAllFormFields());
        
        // Load variants and pricing slabs
        if (config.getId() != null) {
            dto.setVariants(getAllVariants(config.getId()));
            dto.setPricingSlabs(getAllPricingSlabs(config.getId()));
        }
        
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
        
        // Basic fields
        config.setPageTitle(request.getPageTitle());
        config.setPageDescription(request.getPageDescription());
        config.setUploadLabel(request.getUploadLabel());
        config.setDesignPrice(request.getDesignPrice());
        config.setMinQuantity(request.getMinQuantity());
        config.setMaxQuantity(request.getMaxQuantity());
        config.setTermsAndConditions(request.getTermsAndConditions());
        
        // UI Text Fields
        if (request.getUploadButtonText() != null) {
            config.setUploadButtonText(request.getUploadButtonText());
        }
        if (request.getContinueButtonText() != null) {
            config.setContinueButtonText(request.getContinueButtonText());
        }
        if (request.getSubmitButtonText() != null) {
            config.setSubmitButtonText(request.getSubmitButtonText());
        }
        if (request.getAddToCartButtonText() != null) {
            config.setAddToCartButtonText(request.getAddToCartButtonText());
        }
        if (request.getSelectFabricLabel() != null) {
            config.setSelectFabricLabel(request.getSelectFabricLabel());
        }
        if (request.getQuantityLabel() != null) {
            config.setQuantityLabel(request.getQuantityLabel());
        }
        if (request.getInstructions() != null) {
            config.setInstructions(request.getInstructions());
        }
        
        // Business Logic Fields
        config.setGstRate(request.getGstRate());
        config.setHsnCode(request.getHsnCode());
        if (request.getRecommendedFabricIds() != null) {
            config.setRecommendedFabricIds(new ArrayList<>(request.getRecommendedFabricIds()));
        }
        
        configRepository.save(config);
        
        // Update variants
        if (request.getVariants() != null) {
            updateVariants(config, request.getVariants());
        }
        
        // Update pricing slabs
        if (request.getPricingSlabs() != null) {
            updatePricingSlabs(config, request.getPricingSlabs());
        }
        
        // Update form fields
        if (request.getFormFields() != null) {
            updateFormFields(request.getFormFields());
        }
        
        CustomConfigDto dto = toConfigDto(config);
        dto.setFormFields(getAllFormFields());
        if (config.getId() != null) {
            dto.setVariants(getAllVariants(config.getId()));
            dto.setPricingSlabs(getAllPricingSlabs(config.getId()));
        }
        return dto;
    }
    
    @Transactional
    private void updateFormFields(List<CustomConfigRequest.FormFieldRequest> formFieldRequests) {
        // Delete all existing fields first
        formFieldRepository.deleteAll();
        
        // Create new fields from request
        for (int i = 0; i < formFieldRequests.size(); i++) {
            CustomConfigRequest.FormFieldRequest fieldRequest = formFieldRequests.get(i);
            CustomFormField field = new CustomFormField();
            mapFormFieldRequest(fieldRequest, field);
            // Set display order if not provided
            if (field.getDisplayOrder() == null) {
                field.setDisplayOrder(i);
            }
            formFieldRepository.save(field);
        }
    }
    
    @Transactional
    private void updateVariants(CustomProductConfig config, List<CustomConfigRequest.VariantRequest> variantRequests) {
        // Clear existing variants
        config.getVariants().clear();
        variantRepository.deleteAll(variantRepository.findByConfigIdOrderByDisplayOrderAsc(config.getId()));
        
        // Add new variants
        for (CustomConfigRequest.VariantRequest variantRequest : variantRequests) {
            com.sara.ecom.entity.CustomConfigVariant variant = new com.sara.ecom.entity.CustomConfigVariant();
            variant.setConfig(config);
            variant.setType(variantRequest.getType());
            variant.setName(variantRequest.getName());
            variant.setUnit(variantRequest.getUnit());
            variant.setFrontendId(variantRequest.getFrontendId());
            variant.setDisplayOrder(variantRequest.getDisplayOrder() != null ? variantRequest.getDisplayOrder() : 0);
            
            // Add options
            if (variantRequest.getOptions() != null) {
                for (CustomConfigRequest.VariantOptionRequest optionRequest : variantRequest.getOptions()) {
                    com.sara.ecom.entity.CustomConfigVariantOption option = new com.sara.ecom.entity.CustomConfigVariantOption();
                    option.setVariant(variant);
                    option.setValue(optionRequest.getValue());
                    option.setFrontendId(optionRequest.getFrontendId());
                    option.setPriceModifier(optionRequest.getPriceModifier() != null ? 
                        optionRequest.getPriceModifier() : BigDecimal.ZERO);
                    option.setDisplayOrder(optionRequest.getDisplayOrder() != null ? optionRequest.getDisplayOrder() : 0);
                    variant.addOption(option);
                }
            }
            
            config.addVariant(variant);
        }
        
        configRepository.save(config);
    }
    
    @Transactional
    private void updatePricingSlabs(CustomProductConfig config, List<CustomConfigRequest.PricingSlabRequest> slabRequests) {
        // Clear existing slabs
        config.getPricingSlabs().clear();
        pricingSlabRepository.deleteAll(pricingSlabRepository.findByConfigId(config.getId()));
        
        // Add new slabs
        for (CustomConfigRequest.PricingSlabRequest slabRequest : slabRequests) {
            com.sara.ecom.entity.CustomConfigPricingSlab slab = new com.sara.ecom.entity.CustomConfigPricingSlab();
            slab.setConfig(config);
            slab.setMinQuantity(slabRequest.getMinQuantity());
            slab.setMaxQuantity(slabRequest.getMaxQuantity());
            slab.setDisplayOrder(slabRequest.getDisplayOrder() != null ? slabRequest.getDisplayOrder() : 0);
            
            // Set discount type and value
            if (slabRequest.getDiscountType() != null && !slabRequest.getDiscountType().isEmpty()) {
                try {
                    com.sara.ecom.entity.CustomConfigPricingSlab.DiscountType discountType = 
                        com.sara.ecom.entity.CustomConfigPricingSlab.DiscountType.valueOf(slabRequest.getDiscountType().toUpperCase());
                    slab.setDiscountType(discountType);
                    slab.setDiscountValue(slabRequest.getDiscountValue() != null ? 
                        slabRequest.getDiscountValue() : BigDecimal.ZERO);
                } catch (IllegalArgumentException e) {
                    slab.setDiscountType(com.sara.ecom.entity.CustomConfigPricingSlab.DiscountType.FIXED_AMOUNT);
                    slab.setDiscountValue(BigDecimal.ZERO);
                }
            } else {
                slab.setDiscountType(com.sara.ecom.entity.CustomConfigPricingSlab.DiscountType.FIXED_AMOUNT);
                slab.setDiscountValue(BigDecimal.ZERO);
            }
            
            config.addPricingSlab(slab);
        }
        
        configRepository.save(config);
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
        CustomDesignRequest saved = designRequestRepository.save(entity);
        CustomDesignRequestDto dto = toDesignRequestDto(saved);
        
        // Send email notification
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            String requestDate = saved.getCreatedAt() != null 
                ? saved.getCreatedAt().format(formatter) 
                : LocalDateTime.now().format(formatter);
            
            EmailTemplateData.DesignRequestEmailData emailData = new EmailTemplateData.DesignRequestEmailData();
            emailData.setRecipientName(request.getFullName());
            emailData.setRecipientEmail(request.getEmail());
            emailData.setRequestId(saved.getId());
            emailData.setRequestDate(requestDate);
            emailData.setDesignType(request.getDesignType());
            emailData.setDescription(request.getDescription());
            emailData.setReferenceImage(request.getReferenceImage());
            emailData.setStatus(saved.getStatus().name());
            
            emailService.sendDesignRequestSubmittedEmail(emailData);
        } catch (Exception e) {
            // Log error but don't fail request submission
            System.err.println("Failed to send design request email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dto;
    }
    
    @Transactional
    public CustomDesignRequestDto updateDesignRequestStatus(Long id, String status, String adminNotes) {
        CustomDesignRequest request = designRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Design request not found"));
        CustomDesignRequest.Status oldStatus = request.getStatus();
        request.setStatus(CustomDesignRequest.Status.valueOf(status.toUpperCase()));
        if (adminNotes != null) {
            request.setAdminNotes(adminNotes);
        }
        CustomDesignRequest saved = designRequestRepository.save(request);
        CustomDesignRequestDto dto = toDesignRequestDto(saved);
        
        // Send email notification if status changed
        if (oldStatus != saved.getStatus()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
                String requestDate = saved.getCreatedAt() != null 
                    ? saved.getCreatedAt().format(formatter) 
                    : LocalDateTime.now().format(formatter);
                
                EmailTemplateData.DesignRequestEmailData emailData = new EmailTemplateData.DesignRequestEmailData();
                emailData.setRecipientName(saved.getFullName());
                emailData.setRecipientEmail(saved.getEmail());
                emailData.setRequestId(saved.getId());
                emailData.setRequestDate(requestDate);
                emailData.setDesignType(saved.getDesignType());
                emailData.setDescription(saved.getDescription());
                emailData.setReferenceImage(saved.getReferenceImage());
                emailData.setStatus(saved.getStatus().name());
                emailData.setAdminNotes(saved.getAdminNotes());
                
                emailService.sendDesignRequestStatusUpdatedEmail(emailData);
            } catch (Exception e) {
                // Log error but don't fail status update
                System.err.println("Failed to send design request status email: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return dto;
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
        
        // UI Text Fields
        dto.setUploadButtonText(config.getUploadButtonText());
        dto.setContinueButtonText(config.getContinueButtonText());
        dto.setSubmitButtonText(config.getSubmitButtonText());
        dto.setAddToCartButtonText(config.getAddToCartButtonText());
        dto.setSelectFabricLabel(config.getSelectFabricLabel());
        dto.setQuantityLabel(config.getQuantityLabel());
        dto.setInstructions(config.getInstructions());
        
        // Business Logic Fields
        dto.setGstRate(config.getGstRate());
        dto.setHsnCode(config.getHsnCode());
        dto.setRecommendedFabricIds(config.getRecommendedFabricIds() != null ? 
            new ArrayList<>(config.getRecommendedFabricIds()) : new ArrayList<>());
        
        return dto;
    }
    
    // Get all variants for config
    private List<CustomConfigDto.VariantDto> getAllVariants(Long configId) {
        List<com.sara.ecom.entity.CustomConfigVariant> variants = variantRepository.findByConfigIdWithOptions(configId);
        // If empty, try without join fetch
        if (variants.isEmpty()) {
            variants = variantRepository.findByConfigIdOrderByDisplayOrderAsc(configId);
        }
        return variants.stream()
                .map(this::toVariantDto)
                .collect(Collectors.toList());
    }
    
    // Get all pricing slabs for config
    private List<CustomConfigDto.PricingSlabDto> getAllPricingSlabs(Long configId) {
        List<com.sara.ecom.entity.CustomConfigPricingSlab> slabs = pricingSlabRepository.findByConfigId(configId);
        // Sort manually
        slabs.sort((a, b) -> {
            int orderCompare = Integer.compare(
                a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                b.getDisplayOrder() != null ? b.getDisplayOrder() : 0
            );
            if (orderCompare != 0) return orderCompare;
            return Integer.compare(
                a.getMinQuantity() != null ? a.getMinQuantity() : 0,
                b.getMinQuantity() != null ? b.getMinQuantity() : 0
            );
        });
        return slabs.stream()
                .map(this::toPricingSlabDto)
                .collect(Collectors.toList());
    }
    
    private CustomConfigDto.VariantDto toVariantDto(com.sara.ecom.entity.CustomConfigVariant variant) {
        CustomConfigDto.VariantDto dto = new CustomConfigDto.VariantDto();
        dto.setId(variant.getId());
        dto.setType(variant.getType());
        dto.setName(variant.getName());
        dto.setUnit(variant.getUnit());
        dto.setFrontendId(variant.getFrontendId());
        dto.setDisplayOrder(variant.getDisplayOrder());
        
        // Map options
        List<CustomConfigDto.VariantOptionDto> optionDtos = variant.getOptions().stream()
                .map(this::toVariantOptionDto)
                .collect(Collectors.toList());
        dto.setOptions(optionDtos);
        
        return dto;
    }
    
    private CustomConfigDto.VariantOptionDto toVariantOptionDto(com.sara.ecom.entity.CustomConfigVariantOption option) {
        CustomConfigDto.VariantOptionDto dto = new CustomConfigDto.VariantOptionDto();
        dto.setId(option.getId());
        dto.setValue(option.getValue());
        dto.setFrontendId(option.getFrontendId());
        dto.setPriceModifier(option.getPriceModifier());
        dto.setDisplayOrder(option.getDisplayOrder());
        return dto;
    }
    
    private CustomConfigDto.PricingSlabDto toPricingSlabDto(com.sara.ecom.entity.CustomConfigPricingSlab slab) {
        CustomConfigDto.PricingSlabDto dto = new CustomConfigDto.PricingSlabDto();
        dto.setId(slab.getId());
        dto.setMinQuantity(slab.getMinQuantity());
        dto.setMaxQuantity(slab.getMaxQuantity());
        dto.setDiscountType(slab.getDiscountType() != null ? slab.getDiscountType().name() : null);
        dto.setDiscountValue(slab.getDiscountValue());
        dto.setDisplayOrder(slab.getDisplayOrder());
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
