package com.sara.ecom.controller;

import com.sara.ecom.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppWebhookController {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestBody Map<String, Object> rawRequest,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        try {
            // Log signature received from WASender
            if (signature != null) {
                System.out.println("Webhook Signature received: " + signature);
            }
            
            // WASender actual format: { "event": "messages.received", "data": { "messages": { "messageBody": "...", "cleanedSenderPn": "..." } } }
            System.out.println("=== WASender Webhook ===");
            System.out.println("Keys: " + rawRequest.keySet());
            
            // Extract from 'data.messages' object (WASender actual format)
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) rawRequest.get("data");
            
            String fromNumber = null;
            String message = null;
            
            if (data != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messages = (Map<String, Object>) data.get("messages");
                
                if (messages != null) {
                    // Extract phone number from messages.key.cleanedSenderPn (WASender structure)
                    Object keyObj = messages.get("key");
                    if (keyObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> key = (Map<String, Object>) keyObj;
                        
                        // Try cleanedSenderPn first (clean format like "919810167696")
                        Object senderPn = key.get("cleanedSenderPn");
                        if (senderPn != null && !senderPn.toString().trim().isEmpty()) {
                            fromNumber = senderPn.toString().trim();
                            // Add + if not present
                            if (!fromNumber.startsWith("+")) {
                                fromNumber = "+" + fromNumber;
                            }
                        } else {
                            // Fallback to senderPn (format: "919810167696@s.whatsapp.net")
                            senderPn = key.get("senderPn");
                            if (senderPn != null && !senderPn.toString().trim().isEmpty()) {
                                String sender = senderPn.toString().trim();
                                if (sender.contains("@")) {
                                    fromNumber = "+" + sender.split("@")[0];
                                } else {
                                    fromNumber = sender.startsWith("+") ? sender : "+" + sender;
                                }
                            }
                        }
                    }
                    
                    // Extract message - messageBody first (directly in messages)
                    Object msgBody = messages.get("messageBody");
                    if (msgBody != null && !msgBody.toString().trim().isEmpty()) {
                        message = msgBody.toString().trim();
                    } else {
                        // Try nested message.conversation
                        Object messageObj = messages.get("message");
                        if (messageObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                            Object conversation = messageMap.get("conversation");
                            if (conversation != null && !conversation.toString().trim().isEmpty()) {
                                message = conversation.toString().trim();
                            }
                        }
                    }
                }
            }
            
            System.out.println("Extracted - From: " + fromNumber + ", Message: " + (message != null ? message.substring(0, Math.min(50, message.length())) : "null"));
            
            if (fromNumber == null || fromNumber.isEmpty()) {
                System.err.println("ERROR: 'from' not found. Full payload: " + rawRequest);
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "From number required"));
            }
            
            if (message == null || message.isEmpty()) {
                System.err.println("ERROR: 'message' not found. Full payload: " + rawRequest);
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Message required"));
            }
            
            // Process incoming message
            chatbotService.processIncomingMessage(fromNumber, message);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Message processed"));
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    // Alternative webhook format - handle different request structures
    @PostMapping("/webhook/alternative")
    public ResponseEntity<Map<String, String>> receiveWebhookAlternative(@RequestBody Map<String, Object> request) {
        try {
            // Extract from and message from different possible formats
            String from = extractString(request, "from", "phone", "number", "sender", "to");
            String message = extractString(request, "message", "text", "body", "content");
            
            if (from == null || from.trim().isEmpty()) {
                System.err.println("Webhook alternative: Missing 'from' field. Available keys: " + request.keySet());
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing from field"));
            }
            
            if (message == null || message.trim().isEmpty()) {
                System.err.println("Webhook alternative: Missing 'message' field. Available keys: " + request.keySet());
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing message field"));
            }
            
            chatbotService.processIncomingMessage(from.trim(), message.trim());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Message processed"));
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    private String extractString(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }
        return null;
    }
}
