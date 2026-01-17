package com.sara.ecom.controller.admin;

import com.sara.ecom.dto.OrderStatusTemplateDto;
import com.sara.ecom.entity.OrderStatusTemplate;
import com.sara.ecom.repository.OrderStatusTemplateRepository;
import com.sara.ecom.service.WhatsAppNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/whatsapp/order-templates")
public class OrderStatusTemplateController {
    
    @Autowired
    private OrderStatusTemplateRepository templateRepository;
    
    @Autowired
    private WhatsAppNotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<List<OrderStatusTemplateDto>> getAllTemplates() {
        List<OrderStatusTemplateDto> templates = templateRepository.findAllByOrderByStatusTypeAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/{statusType}")
    public ResponseEntity<OrderStatusTemplateDto> getTemplate(@PathVariable String statusType) {
        OrderStatusTemplate.StatusType type = OrderStatusTemplate.StatusType.valueOf(statusType.toUpperCase());
        OrderStatusTemplate template = templateRepository.findByStatusType(type)
                .orElse(null);
        
        if (template == null) {
            // Return default template (will be created automatically when first accessed)
            OrderStatusTemplateDto dto = new OrderStatusTemplateDto();
            dto.setStatusType(type);
            dto.setMessageTemplate(getDefaultTemplate(type));
            dto.setIsEnabled(true); // Default enabled
            return ResponseEntity.ok(dto);
        }
        
        return ResponseEntity.ok(toDto(template));
    }
    
    @PutMapping("/{statusType}")
    public ResponseEntity<OrderStatusTemplateDto> updateTemplate(
            @PathVariable String statusType,
            @RequestBody OrderStatusTemplateDto dto) {
        OrderStatusTemplate.StatusType type = OrderStatusTemplate.StatusType.valueOf(statusType.toUpperCase());
        
        OrderStatusTemplate template = templateRepository.findByStatusType(type)
                .orElse(OrderStatusTemplate.builder()
                        .statusType(type)
                        .build());
        
        template.setMessageTemplate(dto.getMessageTemplate());
        if (dto.getIsEnabled() != null) {
            template.setIsEnabled(dto.getIsEnabled());
        }
        
        template = templateRepository.save(template);
        return ResponseEntity.ok(toDto(template));
    }
    
    @PutMapping("/{statusType}/toggle")
    public ResponseEntity<OrderStatusTemplateDto> toggleTemplate(@PathVariable String statusType) {
        OrderStatusTemplate.StatusType type = OrderStatusTemplate.StatusType.valueOf(statusType.toUpperCase());
        OrderStatusTemplate template = templateRepository.findByStatusType(type)
                .orElseGet(() -> {
                    // Create default template if not exists
                    OrderStatusTemplate newTemplate = OrderStatusTemplate.builder()
                            .statusType(type)
                            .messageTemplate(getDefaultTemplate(type))
                            .isEnabled(true)
                            .build();
                    return templateRepository.save(newTemplate);
                });
        
        template.setIsEnabled(!template.getIsEnabled());
        template = templateRepository.save(template);
        return ResponseEntity.ok(toDto(template));
    }
    
    @PostMapping("/{statusType}/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(@PathVariable String statusType) {
        OrderStatusTemplate.StatusType type = OrderStatusTemplate.StatusType.valueOf(statusType.toUpperCase());
        String preview = notificationService.previewTemplate(type);
        return ResponseEntity.ok(Map.of("preview", preview));
    }
    
    private OrderStatusTemplateDto toDto(OrderStatusTemplate template) {
        return OrderStatusTemplateDto.builder()
                .id(template.getId())
                .statusType(template.getStatusType())
                .messageTemplate(template.getMessageTemplate())
                .isEnabled(template.getIsEnabled())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
    
    private String getDefaultTemplate(OrderStatusTemplate.StatusType type) {
        return switch (type) {
            case ORDER_PLACED -> "Hello {{name}}, your order #{{order_id}} has been placed successfully. Total amount: {{amount}}. Thank you for shopping with us!";
            case ORDER_CONFIRMED -> "Hello {{name}}, your order #{{order_id}} has been confirmed. We're preparing your order for shipment.";
            case PAYMENT_SUCCESS -> "Hello {{name}}, payment of {{amount}} for order #{{order_id}} has been received successfully.";
            case PAYMENT_FAILED -> "Hello {{name}}, payment for order #{{order_id}} failed. Please try again or contact support.";
            case ORDER_SHIPPED -> "Hello {{name}}, your order #{{order_id}} has been shipped. Tracking: {{tracking_number}}";
            case OUT_FOR_DELIVERY -> "Hello {{name}}, your order #{{order_id}} is out for delivery. Expected delivery: {{delivery_date}}";
            case DELIVERED -> "Hello {{name}}, your order #{{order_id}} has been delivered. Thank you for shopping with us!";
            case CANCELLED -> "Hello {{name}}, your order #{{order_id}} has been cancelled. If payment was made, refund will be processed within 5-7 business days.";
            case REFUND_INITIATED -> "Hello {{name}}, refund of {{amount}} for order #{{order_id}} has been initiated. It will reflect in your account within 5-7 business days.";
            case REFUND_COMPLETED -> "Hello {{name}}, refund of {{amount}} for order #{{order_id}} has been completed. Please check your account.";
        };
    }
}
