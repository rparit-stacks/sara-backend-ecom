package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigDto {
    private Long id;
    private Boolean isEnabled;
    private String defaultFallbackReply;
    private String webhookSecret; // WASender webhook secret
    private LocalDateTime updatedAt;
}
