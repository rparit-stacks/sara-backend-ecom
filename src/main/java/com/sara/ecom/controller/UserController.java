package com.sara.ecom.controller;

import com.sara.ecom.dto.UpdateUserRequest;
import com.sara.ecom.dto.UserDto;
import com.sara.ecom.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://design-observer-pro-cyan.vercel.app", "https://studiosara.in", "http://studiosara.in", "https://www.studiosara.in", "http://www.studiosara.in"})
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserDto userDto = userService.getUserProfile(email);
        return ResponseEntity.ok(userDto);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        UserDto userDto = userService.updateUserProfile(email, request);
        return ResponseEntity.ok(userDto);
    }
}
