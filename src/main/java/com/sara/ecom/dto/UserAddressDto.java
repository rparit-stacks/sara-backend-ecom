package com.sara.ecom.dto;

import com.sara.ecom.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String addressType;
    private Boolean isDefault;
    private String landmark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserAddressDto fromEntity(UserAddress address) {
        if (address == null) return null;
        
        return UserAddressDto.builder()
                .id(address.getId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .phoneNumber(address.getPhoneNumber())
                .address(address.getAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .addressType(address.getAddressType() != null ? address.getAddressType().name() : null)
                .isDefault(address.getIsDefault())
                .landmark(address.getLandmark())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
