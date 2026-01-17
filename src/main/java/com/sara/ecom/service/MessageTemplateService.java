package com.sara.ecom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.MessageTemplateDto;
import com.sara.ecom.entity.MessageTemplate;
import com.sara.ecom.repository.MessageTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageTemplateService {
    
    @Autowired
    private MessageTemplateRepository templateRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new message template
     */
    @Transactional
    public MessageTemplateDto createTemplate(MessageTemplateDto dto) {
        MessageTemplate template = new MessageTemplate();
        template.setName(dto.getName());
        template.setContent(dto.getContent());
        
        // Convert variables list to JSON string
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            try {
                template.setVariables(objectMapper.writeValueAsString(dto.getVariables()));
            } catch (Exception e) {
                template.setVariables("[]");
            }
        } else {
            template.setVariables("[]");
        }
        
        template = templateRepository.save(template);
        return toDto(template);
    }
    
    /**
     * Get all templates
     */
    public List<MessageTemplateDto> getAllTemplates() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get template by ID
     */
    public MessageTemplateDto getTemplateById(Long id) {
        MessageTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return toDto(template);
    }
    
    /**
     * Update template
     */
    @Transactional
    public MessageTemplateDto updateTemplate(Long id, MessageTemplateDto dto) {
        MessageTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        template.setName(dto.getName());
        template.setContent(dto.getContent());
        
        // Convert variables list to JSON string
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            try {
                template.setVariables(objectMapper.writeValueAsString(dto.getVariables()));
            } catch (Exception e) {
                template.setVariables("[]");
            }
        } else {
            template.setVariables("[]");
        }
        
        template = templateRepository.save(template);
        return toDto(template);
    }
    
    /**
     * Delete template
     */
    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }
    
    /**
     * Render template with variables
     */
    public String renderTemplate(Long templateId, Map<String, String> variables) {
        MessageTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        String content = template.getContent();
        
        // Replace variables
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                content = content.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        return content;
    }
    
    /**
     * Render template by content string with variables
     */
    public String renderTemplateContent(String templateContent, Map<String, String> variables) {
        String content = templateContent;
        
        // Replace variables
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                content = content.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        return content;
    }
    
    private MessageTemplateDto toDto(MessageTemplate template) {
        MessageTemplateDto dto = new MessageTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setContent(template.getContent());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        
        // Parse variables from JSON string
        if (template.getVariables() != null && !template.getVariables().trim().isEmpty()) {
            try {
                List<String> variables = objectMapper.readValue(template.getVariables(), new TypeReference<List<String>>() {});
                dto.setVariables(variables);
            } catch (Exception e) {
                dto.setVariables(List.of());
            }
        } else {
            dto.setVariables(List.of());
        }
        
        return dto;
    }
}
