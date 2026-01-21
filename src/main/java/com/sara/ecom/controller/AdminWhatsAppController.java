package com.sara.ecom.controller;

import com.sara.ecom.dto.*;
import com.sara.ecom.entity.*;
import com.sara.ecom.repository.*;
import com.sara.ecom.service.BusinessConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/whatsapp")
public class AdminWhatsAppController {
    
    @Autowired
    private WhatsAppTemplateRepository templateRepository;
    
    @Autowired
    private CustomOrderStatusRepository customStatusRepository;
    
    @Autowired
    private WhatsAppNotificationLogRepository logRepository;
    
    @Autowired
    private BusinessConfigService businessConfigService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Template endpoints
    @GetMapping("/templates")
    public ResponseEntity<List<WhatsAppTemplateDto>> getAllTemplates() {
        List<WhatsAppTemplate> templates = templateRepository.findAll();
        List<WhatsAppTemplateDto> dtos = templates.stream()
            .map(this::toTemplateDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/templates")
    public ResponseEntity<WhatsAppTemplateDto> createTemplate(@RequestBody WhatsAppTemplateDto dto) {
        WhatsAppTemplate template = new WhatsAppTemplate();
        template.setStatusType(dto.getStatusType());
        template.setTemplateName(dto.getTemplateName());
        template.setMessageTemplate(dto.getMessageTemplate());
        template.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        
        WhatsAppTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toTemplateDto(saved));
    }
    
    @PutMapping("/templates/{id}")
    public ResponseEntity<WhatsAppTemplateDto> updateTemplate(
            @PathVariable Long id,
            @RequestBody WhatsAppTemplateDto dto) {
        WhatsAppTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        template.setStatusType(dto.getStatusType());
        template.setTemplateName(dto.getTemplateName());
        template.setMessageTemplate(dto.getMessageTemplate());
        if (dto.getIsEnabled() != null) {
            template.setIsEnabled(dto.getIsEnabled());
        }
        
        WhatsAppTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toTemplateDto(saved));
    }
    
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // Custom Status endpoints
    @GetMapping("/custom-statuses")
    public ResponseEntity<List<CustomOrderStatusDto>> getAllCustomStatuses() {
        List<CustomOrderStatus> statuses = customStatusRepository.findAll();
        List<CustomOrderStatusDto> dtos = statuses.stream()
            .map(this::toCustomStatusDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/custom-statuses")
    public ResponseEntity<CustomOrderStatusDto> createCustomStatus(@RequestBody CustomOrderStatusDto dto) {
        // Check if status name already exists
        if (customStatusRepository.findByStatusName(dto.getStatusName()).isPresent()) {
            throw new RuntimeException("Custom status with this name already exists");
        }
        
        CustomOrderStatus status = new CustomOrderStatus();
        status.setStatusName(dto.getStatusName());
        status.setDisplayName(dto.getDisplayName());
        status.setTemplateId(dto.getTemplateId());
        status.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        CustomOrderStatus saved = customStatusRepository.save(status);
        return ResponseEntity.ok(toCustomStatusDto(saved));
    }
    
    @PutMapping("/custom-statuses/{id}")
    public ResponseEntity<CustomOrderStatusDto> updateCustomStatus(
            @PathVariable Long id,
            @RequestBody CustomOrderStatusDto dto) {
        CustomOrderStatus status = customStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom status not found"));
        
        // Check if status name is being changed and if it conflicts
        if (!status.getStatusName().equals(dto.getStatusName())) {
            if (customStatusRepository.findByStatusName(dto.getStatusName()).isPresent()) {
                throw new RuntimeException("Custom status with this name already exists");
            }
        }
        
        status.setStatusName(dto.getStatusName());
        status.setDisplayName(dto.getDisplayName());
        status.setTemplateId(dto.getTemplateId());
        if (dto.getIsActive() != null) {
            status.setIsActive(dto.getIsActive());
        }
        
        CustomOrderStatus saved = customStatusRepository.save(status);
        return ResponseEntity.ok(toCustomStatusDto(saved));
    }
    
    @DeleteMapping("/custom-statuses/{id}")
    public ResponseEntity<Void> deleteCustomStatus(@PathVariable Long id) {
        customStatusRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // Notification Log endpoints
    @GetMapping("/logs")
    public ResponseEntity<Page<WhatsAppNotificationLogDto>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WhatsAppNotificationLog> logs = logRepository.findAllByOrderByCreatedAtDesc(pageable);
        Page<WhatsAppNotificationLogDto> dtos = logs.map(this::toLogDto);
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/logs/order/{orderId}")
    public ResponseEntity<List<WhatsAppNotificationLogDto>> getLogsByOrder(@PathVariable Long orderId) {
        List<WhatsAppNotificationLog> logs = logRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        List<WhatsAppNotificationLogDto> dtos = logs.stream()
            .map(this::toLogDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    // Config endpoints
    @GetMapping("/config")
    public ResponseEntity<WhatsAppConfigDto> getConfig() {
        BusinessConfigDto config = businessConfigService.getConfig();
        WhatsAppConfigDto dto = new WhatsAppConfigDto();
        
        // Mask API key - show last 6 characters if key exists
        String apiKey = config.getDoubletickApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("***API_KEY_SET***")) {
            // Show last 6 characters with mask
            if (apiKey.length() > 6) {
                String maskedKey = "***" + apiKey.substring(apiKey.length() - 6);
                dto.setDoubletickApiKey(maskedKey);
            } else {
                dto.setDoubletickApiKey("***" + apiKey);
            }
        } else {
            dto.setDoubletickApiKey(null);
        }
        
        dto.setDoubletickSenderNumber(config.getDoubletickSenderNumber());
        dto.setDoubletickTemplateName(config.getDoubletickTemplateName());
        dto.setDoubletickEnabled(config.getDoubletickEnabled() != null ? config.getDoubletickEnabled() : false);
        return ResponseEntity.ok(dto);
    }
    
    @PutMapping("/config")
    public ResponseEntity<WhatsAppConfigDto> updateConfig(@RequestBody WhatsAppConfigDto dto) {
        // Get config with actual API key to preserve it
        BusinessConfigDto existingConfig = businessConfigService.getConfigWithApiKey();
        
        // Only update API key if it's a new full key (not masked, not null, not empty)
        String newApiKey = dto.getDoubletickApiKey();
        if (newApiKey != null && !newApiKey.trim().isEmpty() && !newApiKey.startsWith("***")) {
            // It's a new full API key, update it
            existingConfig.setDoubletickApiKey(newApiKey.trim());
        }
        // If null, empty, or starts with "***", keep existing key (already in existingConfig)
        
        // Update sender number if provided
        if (dto.getDoubletickSenderNumber() != null && !dto.getDoubletickSenderNumber().trim().isEmpty()) {
            existingConfig.setDoubletickSenderNumber(dto.getDoubletickSenderNumber().trim());
        }
        
        // Update template name if provided
        if (dto.getDoubletickTemplateName() != null && !dto.getDoubletickTemplateName().trim().isEmpty()) {
            existingConfig.setDoubletickTemplateName(dto.getDoubletickTemplateName().trim());
        }
        
        // Update enabled status if provided
        if (dto.getDoubletickEnabled() != null) {
            existingConfig.setDoubletickEnabled(dto.getDoubletickEnabled());
        }
        
        // Save the config (existing API key will be preserved if not updated)
        BusinessConfigDto saved = businessConfigService.saveConfig(existingConfig);
        
        // Return masked API key in response
        WhatsAppConfigDto response = new WhatsAppConfigDto();
        String savedApiKey = saved.getDoubletickApiKey();
        if (savedApiKey != null && !savedApiKey.trim().isEmpty() && !savedApiKey.equals("***API_KEY_SET***")) {
            if (savedApiKey.length() > 6) {
                response.setDoubletickApiKey("***" + savedApiKey.substring(savedApiKey.length() - 6));
            } else {
                response.setDoubletickApiKey("***" + savedApiKey);
            }
        } else {
            response.setDoubletickApiKey(null);
        }
        response.setDoubletickSenderNumber(saved.getDoubletickSenderNumber());
        response.setDoubletickTemplateName(saved.getDoubletickTemplateName());
        response.setDoubletickEnabled(saved.getDoubletickEnabled());
        return ResponseEntity.ok(response);
    }
    
    // Helper methods
    private WhatsAppTemplateDto toTemplateDto(WhatsAppTemplate template) {
        WhatsAppTemplateDto dto = new WhatsAppTemplateDto();
        dto.setId(template.getId());
        dto.setStatusType(template.getStatusType());
        dto.setTemplateName(template.getTemplateName());
        dto.setMessageTemplate(template.getMessageTemplate());
        dto.setIsEnabled(template.getIsEnabled());
        if (template.getCreatedAt() != null) {
            dto.setCreatedAt(template.getCreatedAt().format(DATE_FORMATTER));
        }
        if (template.getUpdatedAt() != null) {
            dto.setUpdatedAt(template.getUpdatedAt().format(DATE_FORMATTER));
        }
        return dto;
    }
    
    private CustomOrderStatusDto toCustomStatusDto(CustomOrderStatus status) {
        CustomOrderStatusDto dto = new CustomOrderStatusDto();
        dto.setId(status.getId());
        dto.setStatusName(status.getStatusName());
        dto.setDisplayName(status.getDisplayName());
        dto.setTemplateId(status.getTemplateId());
        dto.setIsActive(status.getIsActive());
        if (status.getCreatedAt() != null) {
            dto.setCreatedAt(status.getCreatedAt().format(DATE_FORMATTER));
        }
        if (status.getUpdatedAt() != null) {
            dto.setUpdatedAt(status.getUpdatedAt().format(DATE_FORMATTER));
        }
        return dto;
    }
    
    private WhatsAppNotificationLogDto toLogDto(WhatsAppNotificationLog log) {
        WhatsAppNotificationLogDto dto = new WhatsAppNotificationLogDto();
        dto.setId(log.getId());
        dto.setOrderId(log.getOrderId());
        dto.setPhoneNumber(log.getPhoneNumber());
        dto.setMessage(log.getMessage());
        dto.setDeliveryStatus(log.getDeliveryStatus());
        dto.setMessageId(log.getMessageId());
        dto.setErrorMessage(log.getErrorMessage());
        if (log.getCreatedAt() != null) {
            dto.setCreatedAt(log.getCreatedAt().format(DATE_FORMATTER));
        }
        return dto;
    }
}
