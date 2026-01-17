package com.sara.ecom.service;

import com.sara.ecom.entity.SentMessage;
import com.sara.ecom.entity.WASenderAccount;
import com.sara.ecom.repository.SentMessageRepository;
import com.sara.ecom.repository.WASenderAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WASenderService {
    
    @Autowired
    private WASenderAccountRepository accountRepository;
    
    @Autowired
    private SentMessageRepository sentMessageRepository;
    
    @Value("${wasender.api.base-url:https://wasenderapi.com}")
    private String wasenderApiBaseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get the currently active WASender account
     */
    public WASenderAccount getActiveAccount() {
        return accountRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active WASender account found. Please activate an account in admin panel."));
    }
    
    /**
     * Send a message via WASender API
     */
    @Transactional
    public SentMessage sendMessage(String phoneNumber, String message, SentMessage.MessageType messageType, Long orderId) {
        // Validate phone number
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        // Validate message
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        WASenderAccount activeAccount = getActiveAccount();
        
        // Create SentMessage record
        SentMessage sentMessage = SentMessage.builder()
                .recipientNumber(phoneNumber.trim())
                .messageContent(message.trim())
                .messageType(messageType)
                .status(SentMessage.Status.PENDING)
                .orderId(orderId)
                .build();
        sentMessage = sentMessageRepository.save(sentMessage);
        
        try {
            // Prepare request
            String url = wasenderApiBaseUrl + "/api/send-message";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(activeAccount.getBearerToken());
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", phoneNumber);
            requestBody.put("text", message);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            // Update sent message
            sentMessage.setStatus(SentMessage.Status.SENT);
            sentMessage.setSentAt(LocalDateTime.now());
            sentMessage.setWasenderResponse(response.getBody());
            sentMessage = sentMessageRepository.save(sentMessage);
            
            return sentMessage;
            
        } catch (Exception e) {
            // Update sent message with error
            sentMessage.setStatus(SentMessage.Status.FAILED);
            sentMessage.setWasenderResponse("Error: " + e.getMessage());
            sentMessage = sentMessageRepository.save(sentMessage);
            
            System.err.println("Failed to send WhatsApp message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send bulk messages
     */
    @Transactional
    public List<SentMessage> sendBulkMessages(List<String> phoneNumbers, String message, SentMessage.MessageType messageType) {
        return phoneNumbers.stream()
                .map(phoneNumber -> {
                    try {
                        return sendMessage(phoneNumber, message, messageType, null);
                    } catch (Exception e) {
                        System.err.println("Failed to send message to " + phoneNumber + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(msg -> msg != null)
                .toList();
    }
    
    /**
     * Test connection to WASender API
     */
    public boolean testConnection(String bearerToken) {
        try {
            String url = wasenderApiBaseUrl + "/api/send-message";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);
            
            // Send test message to a dummy number (will fail but we check if API is reachable)
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", "1234567890"); // Dummy number
            requestBody.put("text", "Test connection");
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call - we expect it might fail but we check if API is accessible
            try {
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            } catch (Exception e) {
                // If we get a response (even error), API is reachable
                // If it's a connection error, token might be invalid or API down
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("401") || errorMsg.contains("Unauthorized"))) {
                    return false; // Invalid token
                }
                // Other errors might mean API is reachable but request failed (which is OK for test)
                return true;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Activate a WASender account (deactivates all others)
     */
    @Transactional
    public void activateAccount(Long accountId) {
        // Deactivate all accounts
        List<WASenderAccount> allAccounts = accountRepository.findAll();
        for (WASenderAccount account : allAccounts) {
            account.setIsActive(false);
            accountRepository.save(account);
        }
        
        // Activate the selected account
        WASenderAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setIsActive(true);
        accountRepository.save(account);
    }
}
