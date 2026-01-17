package com.sara.ecom.controller.admin;

import com.sara.ecom.dto.BroadcastRequest;
import com.sara.ecom.dto.SendMessageRequest;
import com.sara.ecom.dto.SentMessageDto;
import com.sara.ecom.entity.SentMessage;
import com.sara.ecom.repository.SentMessageRepository;
import com.sara.ecom.service.MessageTemplateService;
import com.sara.ecom.service.WASenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/whatsapp/messages")
public class MessageController {
    
    @Autowired
    private WASenderService wasenderService;
    
    @Autowired
    private MessageTemplateService templateService;
    
    @Autowired
    private SentMessageRepository sentMessageRepository;
    
    @PostMapping("/send")
    public ResponseEntity<SentMessageDto> sendMessage(@RequestBody SendMessageRequest request) {
        String message = request.getMessage();
        
        // If template is provided, render it
        if (request.getTemplateId() != null) {
            message = templateService.renderTemplate(request.getTemplateId(), request.getVariables());
        } else if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            // Render variables in message directly
            message = templateService.renderTemplateContent(message, request.getVariables());
        }
        
        SentMessage sentMessage = wasenderService.sendMessage(
                request.getPhoneNumber(),
                message,
                SentMessage.MessageType.MANUAL,
                null
        );
        
        return ResponseEntity.ok(toDto(sentMessage));
    }
    
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcast(@RequestBody BroadcastRequest request) {
        String message = request.getMessage();
        
        // If template is provided, render it
        if (request.getTemplateId() != null) {
            message = templateService.renderTemplate(request.getTemplateId(), request.getVariables());
        } else if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            // Render variables in message directly
            message = templateService.renderTemplateContent(message, request.getVariables());
        }
        
        List<SentMessage> sentMessages = wasenderService.sendBulkMessages(
                request.getPhoneNumbers(),
                message,
                SentMessage.MessageType.BROADCAST
        );
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sent", sentMessages.size(),
                "total", request.getPhoneNumbers().size()
        ));
    }
    
    @GetMapping
    public ResponseEntity<List<SentMessageDto>> getMessageHistory(
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String status) {
        List<SentMessage> messages;
        
        if (messageType != null && status != null) {
            SentMessage.MessageType type = SentMessage.MessageType.valueOf(messageType.toUpperCase());
            SentMessage.Status stat = SentMessage.Status.valueOf(status.toUpperCase());
            messages = sentMessageRepository.findAll().stream()
                    .filter(m -> m.getMessageType() == type && m.getStatus() == stat)
                    .collect(Collectors.toList());
        } else if (messageType != null) {
            SentMessage.MessageType type = SentMessage.MessageType.valueOf(messageType.toUpperCase());
            messages = sentMessageRepository.findByMessageTypeOrderByCreatedAtDesc(type);
        } else if (status != null) {
            SentMessage.Status stat = SentMessage.Status.valueOf(status.toUpperCase());
            messages = sentMessageRepository.findByStatusOrderByCreatedAtDesc(stat);
        } else {
            messages = sentMessageRepository.findAllByOrderByCreatedAtDesc();
        }
        
        List<SentMessageDto> dtos = messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SentMessageDto> getMessageById(@PathVariable Long id) {
        SentMessage message = sentMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        return ResponseEntity.ok(toDto(message));
    }
    
    private SentMessageDto toDto(SentMessage message) {
        return SentMessageDto.builder()
                .id(message.getId())
                .recipientNumber(message.getRecipientNumber())
                .messageContent(message.getMessageContent())
                .messageType(message.getMessageType())
                .status(message.getStatus())
                .wasenderResponse(message.getWasenderResponse())
                .orderId(message.getOrderId())
                .sentAt(message.getSentAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
