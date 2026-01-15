package com.sara.ecom.controller;

import com.sara.ecom.dto.UserDto;
import com.sara.ecom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(@RequestParam(required = false) String status) {
        List<UserDto> users;
        if (status != null && !status.isEmpty()) {
            users = userService.getUsersByStatus(status);
        } else {
            users = userService.getAllUsers();
        }
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{email}/status")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable String email,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(userService.updateUserStatus(email, status));
    }
}
