package com.sara.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotBlank(message = "State is required")
    private String state;
    
    @NotBlank(message = "ZIP code is required")
    private String zipCode;
    
    @NotBlank(message = "Country is required")
    private String country;
    
    private String addressType; // HOME, WORK, OTHER
    
    private Boolean isDefault;
    
    private String landmark;
    
    private String gstin; // Optional GSTIN for B2B customers
}
