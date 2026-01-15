package com.sara.ecom.service;

import com.sara.ecom.dto.AddressRequest;
import com.sara.ecom.dto.UserAddressDto;
import com.sara.ecom.entity.User;
import com.sara.ecom.entity.UserAddress;
import com.sara.ecom.repository.UserAddressRepository;
import com.sara.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public List<UserAddressDto> getUserAddresses(String userEmail) {
        List<UserAddress> addresses = addressRepository.findByUserEmailOrderByIsDefaultDescCreatedAtDesc(userEmail);
        return addresses.stream()
                .map(UserAddressDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public UserAddressDto getAddressById(Long id, String userEmail) {
        UserAddress address = addressRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        return UserAddressDto.fromEntity(address);
    }
    
    @Transactional
    public UserAddressDto createAddress(String userEmail, AddressRequest request) {
        // Verify user exists
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // If this is set as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultAddresses(userEmail);
        }
        
        UserAddress address = UserAddress.builder()
                .userEmail(userEmail)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .addressType(request.getAddressType() != null ? 
                        UserAddress.AddressType.valueOf(request.getAddressType().toUpperCase()) : 
                        UserAddress.AddressType.HOME)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .landmark(request.getLandmark())
                .build();
        
        address = addressRepository.save(address);
        return UserAddressDto.fromEntity(address);
    }
    
    @Transactional
    public UserAddressDto updateAddress(Long id, String userEmail, AddressRequest request) {
        UserAddress address = addressRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // If setting as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.clearDefaultAddresses(userEmail);
        }
        
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddress(request.getAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        if (request.getAddressType() != null) {
            address.setAddressType(UserAddress.AddressType.valueOf(request.getAddressType().toUpperCase()));
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        if (request.getLandmark() != null) {
            address.setLandmark(request.getLandmark());
        }
        
        address = addressRepository.save(address);
        return UserAddressDto.fromEntity(address);
    }
    
    @Transactional
    public void deleteAddress(Long id, String userEmail) {
        UserAddress address = addressRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepository.delete(address);
    }
    
    @Transactional
    public UserAddressDto setDefaultAddress(Long id, String userEmail) {
        UserAddress address = addressRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Clear all defaults
        addressRepository.clearDefaultAddresses(userEmail);
        
        // Set this as default
        address.setIsDefault(true);
        address = addressRepository.save(address);
        
        return UserAddressDto.fromEntity(address);
    }
}
