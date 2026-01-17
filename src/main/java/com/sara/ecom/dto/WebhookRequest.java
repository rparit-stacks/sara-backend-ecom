package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {
    private String from; // Phone number
    private String message; // Message content
    private String timestamp; // Optional timestamp
}
