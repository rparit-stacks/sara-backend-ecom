package com.sara.ecom.service;

import com.sara.ecom.dto.AdminAuthResponse;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminLoginRequest;
import com.sara.ecom.dto.AdminSignupRequest;
import com.sara.ecom.entity.Admin;
import com.sara.ecom.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class AdminAuthService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private JwtService jwtService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Hardcoded authentication code for admin signup
    private static final String ADMIN_AUTH_CODE = "VAGAT-90BH-8UIUUI";
    
    @PostConstruct
    public void init() {
        // Create default admin if not exists
        if (!adminRepository.existsByEmail("admin@studiosara.com")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin@sara"));
            admin.setName("Administrator");
            admin.setEmail("admin@studiosara.com");
            admin.setStatus(Admin.Status.ACTIVE);
            adminRepository.save(admin);
        }
    }
    
    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        if (admin.getStatus() != Admin.Status.ACTIVE) {
            throw new RuntimeException("Admin account is inactive");
        }
        
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);
        
        String token = jwtService.generateAdminToken(admin.getEmail(), admin.getId());
        return new AdminAuthResponse(token, toAdminDto(admin));
    }
    
    public AdminDto getCurrentAdmin(String token) {
        String email = jwtService.extractEmail(token);
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return toAdminDto(admin);
    }
    
    @Transactional
    public AdminAuthResponse signup(AdminSignupRequest request) {
        // Validate authentication code
        if (!ADMIN_AUTH_CODE.equals(request.getAuthCode())) {
            throw new RuntimeException("Invalid authentication code");
        }
        
        // Check if email already exists
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered. Please login instead.");
        }
        
        // Create new admin
        Admin admin = new Admin();
        admin.setEmail(request.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setName(request.getName());
        // Generate username from email (part before @)
        String baseUsername = request.getEmail().split("@")[0];
        String username = baseUsername;
        int counter = 1;
        // Ensure username is unique
        while (adminRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        admin.setUsername(username);
        admin.setStatus(Admin.Status.ACTIVE);
        
        admin = adminRepository.save(admin);
        
        // Auto login after signup
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);
        
        String token = jwtService.generateAdminToken(admin.getEmail(), admin.getId());
        return new AdminAuthResponse(token, toAdminDto(admin));
    }
    
    public boolean isAdminToken(String token) {
        return jwtService.isAdminToken(token);
    }
    
    private AdminDto toAdminDto(Admin admin) {
        AdminDto dto = new AdminDto();
        dto.setId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setName(admin.getName());
        dto.setEmail(admin.getEmail());
        dto.setStatus(admin.getStatus().name());
        dto.setLastLogin(admin.getLastLogin());
        return dto;
    }
}
