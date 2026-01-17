package com.sara.ecom.dto;

import com.sara.ecom.entity.OrderStatusTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusTemplateDto {
    private Long id;
    private OrderStatusTemplate.StatusType statusType;
    private String messageTemplate;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
