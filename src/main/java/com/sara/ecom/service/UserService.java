package com.sara.ecom.service;

import com.sara.ecom.dto.UpdateUserRequest;
import com.sara.ecom.dto.UserDto;
import com.sara.ecom.entity.User;
import com.sara.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserDto getUserProfile(String email) {
        // Normalize email to lowercase
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDto.fromEntity(user);
    }
    
    @Transactional
    public UserDto updateUserProfile(String email, UpdateUserRequest request) {
        // Normalize email to lowercase
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            user.setZipCode(request.getZipCode());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        
        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }
    
    // Admin methods
    public List<UserDto> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<UserDto> getUsersByStatus(String status) {
        User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
        return userRepository.findByStatusOrderByCreatedAtDesc(userStatus).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public UserDto updateUserStatus(String email, String status) {
        // Normalize email to lowercase
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }
    
    public long countAllUsers() {
        return userRepository.count();
    }
    
    public long countActiveUsers() {
        return userRepository.countByStatus(User.UserStatus.ACTIVE);
    }
}
