package com.sara.ecom.controller;

import com.sara.ecom.dto.AddressRequest;
import com.sara.ecom.dto.UserAddressDto;
import com.sara.ecom.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserAddressController {
    
    private final UserAddressService addressService;
    
    @GetMapping
    public ResponseEntity<List<UserAddressDto>> getUserAddresses(Authentication authentication) {
        String userEmail = authentication.getName();
        List<UserAddressDto> addresses = addressService.getUserAddresses(userEmail);
        return ResponseEntity.ok(addresses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserAddressDto> getAddressById(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        UserAddressDto address = addressService.getAddressById(id, userEmail);
        return ResponseEntity.ok(address);
    }
    
    @PostMapping
    public ResponseEntity<UserAddressDto> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        UserAddressDto address = addressService.createAddress(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserAddressDto> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        UserAddressDto address = addressService.updateAddress(id, userEmail, request);
        return ResponseEntity.ok(address);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        addressService.deleteAddress(id, userEmail);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/default")
    public ResponseEntity<UserAddressDto> setDefaultAddress(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        UserAddressDto address = addressService.setDefaultAddress(id, userEmail);
        return ResponseEntity.ok(address);
    }
}
