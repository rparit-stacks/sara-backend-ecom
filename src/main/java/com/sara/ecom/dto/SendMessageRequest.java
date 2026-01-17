package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String phoneNumber;
    private String message;
    private Long templateId; // Optional: use template
    private Map<String, String> variables; // For template variables
}
