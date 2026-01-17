package com.sara.ecom.controller.admin;

import com.sara.ecom.dto.ChatbotConfigDto;
import com.sara.ecom.dto.ChatbotRuleDto;
import com.sara.ecom.entity.ChatbotConfig;
import com.sara.ecom.entity.ChatbotRule;
import com.sara.ecom.repository.ChatbotConfigRepository;
import com.sara.ecom.repository.ChatbotRuleRepository;
import com.sara.ecom.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/whatsapp/chatbot")
public class ChatbotController {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @Autowired
    private ChatbotConfigRepository configRepository;
    
    @Autowired
    private ChatbotRuleRepository ruleRepository;
    
    @GetMapping("/config")
    public ResponseEntity<ChatbotConfigDto> getConfig() {
        ChatbotConfig config = chatbotService.getChatbotConfig();
        return ResponseEntity.ok(toConfigDto(config));
    }
    
    @PutMapping("/config")
    public ResponseEntity<ChatbotConfigDto> updateConfig(@RequestBody ChatbotConfigDto dto) {
        ChatbotConfig config = chatbotService.getChatbotConfig();
        config.setIsEnabled(dto.getIsEnabled());
        config.setDefaultFallbackReply(dto.getDefaultFallbackReply());
        if (dto.getWebhookSecret() != null) {
            config.setWebhookSecret(dto.getWebhookSecret());
        }
        config = configRepository.save(config);
        return ResponseEntity.ok(toConfigDto(config));
    }
    
    @PostMapping("/rules")
    public ResponseEntity<ChatbotRuleDto> createRule(@RequestBody ChatbotRuleDto dto) {
        ChatbotRule rule = ChatbotRule.builder()
                .keyword(dto.getKeyword())
                .userMessage(dto.getUserMessage())
                .botReply(dto.getBotReply())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                .build();
        
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(toRuleDto(rule));
    }
    
    @GetMapping("/rules")
    public ResponseEntity<List<ChatbotRuleDto>> getAllRules() {
        List<ChatbotRuleDto> rules = ruleRepository.findAllByOrderByPriorityDesc().stream()
                .map(this::toRuleDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/rules/{id}")
    public ResponseEntity<ChatbotRuleDto> getRuleById(@PathVariable Long id) {
        ChatbotRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        return ResponseEntity.ok(toRuleDto(rule));
    }
    
    @PutMapping("/rules/{id}")
    public ResponseEntity<ChatbotRuleDto> updateRule(@PathVariable Long id, @RequestBody ChatbotRuleDto dto) {
        ChatbotRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        rule.setKeyword(dto.getKeyword());
        rule.setUserMessage(dto.getUserMessage());
        rule.setBotReply(dto.getBotReply());
        if (dto.getIsActive() != null) {
            rule.setIsActive(dto.getIsActive());
        }
        if (dto.getPriority() != null) {
            rule.setPriority(dto.getPriority());
        }
        
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(toRuleDto(rule));
    }
    
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<ChatbotRuleDto> toggleRule(@PathVariable Long id) {
        ChatbotRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        rule.setIsActive(!rule.getIsActive());
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(toRuleDto(rule));
    }
    
    private ChatbotConfigDto toConfigDto(ChatbotConfig config) {
        return ChatbotConfigDto.builder()
                .id(config.getId())
                .isEnabled(config.getIsEnabled())
                .defaultFallbackReply(config.getDefaultFallbackReply())
                .webhookSecret(config.getWebhookSecret())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
    
    private ChatbotRuleDto toRuleDto(ChatbotRule rule) {
        return ChatbotRuleDto.builder()
                .id(rule.getId())
                .keyword(rule.getKeyword())
                .userMessage(rule.getUserMessage())
                .botReply(rule.getBotReply())
                .isActive(rule.getIsActive())
                .priority(rule.getPriority())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
