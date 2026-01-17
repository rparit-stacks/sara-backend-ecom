package com.sara.ecom.service;

import com.sara.ecom.entity.ChatbotConfig;
import com.sara.ecom.entity.ChatbotRule;
import com.sara.ecom.entity.SentMessage;
import com.sara.ecom.repository.ChatbotConfigRepository;
import com.sara.ecom.repository.ChatbotRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatbotService {
    
    @Autowired
    private ChatbotConfigRepository configRepository;
    
    @Autowired
    private ChatbotRuleRepository ruleRepository;
    
    @Autowired
    private WASenderService wasenderService;
    
    /**
     * Get chatbot configuration (create default if not exists)
     */
    public ChatbotConfig getChatbotConfig() {
        Optional<ChatbotConfig> configOpt = configRepository.findFirstByOrderByIdAsc();
        if (configOpt.isPresent()) {
            return configOpt.get();
        }
        
        // Create default config
        ChatbotConfig defaultConfig = ChatbotConfig.builder()
                .isEnabled(false)
                .defaultFallbackReply("Thank you for your message. Our team will get back to you soon.")
                .build();
        return configRepository.save(defaultConfig);
    }
    
    /**
     * Check if chatbot is enabled
     */
    public boolean isChatbotEnabled() {
        ChatbotConfig config = getChatbotConfig();
        return config.getIsEnabled() != null && config.getIsEnabled();
    }
    
    /**
     * Process incoming message and find matching rule
     */
    public ChatbotRule findMatchingRule(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        
        String normalizedMessage = message.toLowerCase().trim();
        
        // Get all active rules ordered by priority
        List<ChatbotRule> activeRules = ruleRepository.findByIsActiveTrueOrderByPriorityDesc();
        
        for (ChatbotRule rule : activeRules) {
            String keyword = rule.getKeyword();
            if (keyword == null) continue;
            
            String normalizedKeyword = keyword.toLowerCase().trim();
            
            // Check if message contains keyword
            if (normalizedMessage.contains(normalizedKeyword)) {
                // If userMessage pattern is specified, check if it matches
                if (rule.getUserMessage() != null && !rule.getUserMessage().trim().isEmpty()) {
                    String pattern = rule.getUserMessage().toLowerCase().trim();
                    // Simple pattern matching - can be enhanced with regex
                    if (normalizedMessage.contains(pattern) || normalizedMessage.equals(pattern)) {
                        return rule;
                    }
                } else {
                    // No pattern specified, keyword match is enough
                    return rule;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Process incoming webhook message and send reply if chatbot is enabled
     */
    @Transactional
    public void processIncomingMessage(String fromNumber, String message) {
        if (!isChatbotEnabled()) {
            return; // Chatbot is disabled
        }
        
        ChatbotRule matchingRule = findMatchingRule(message);
        ChatbotConfig config = getChatbotConfig();
        
        String replyMessage;
        
        if (matchingRule != null) {
            replyMessage = matchingRule.getBotReply();
        } else {
            // Use fallback reply
            replyMessage = config.getDefaultFallbackReply();
            if (replyMessage == null || replyMessage.trim().isEmpty()) {
                replyMessage = "Thank you for your message. Our team will get back to you soon.";
            }
        }
        
        // Send reply via WASender
        try {
            // Validate phone number before sending
            if (fromNumber == null || fromNumber.trim().isEmpty()) {
                System.err.println("Cannot send chatbot reply: fromNumber is null or empty");
                return;
            }
            
            wasenderService.sendMessage(fromNumber, replyMessage, SentMessage.MessageType.CHATBOT_REPLY, null);
        } catch (Exception e) {
            System.err.println("Failed to send chatbot reply: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
