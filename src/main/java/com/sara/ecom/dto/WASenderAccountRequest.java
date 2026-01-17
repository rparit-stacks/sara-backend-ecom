package com.sara.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WASenderAccountRequest {
    private String accountName;
    private String bearerToken;
    private String whatsappNumber;
}
