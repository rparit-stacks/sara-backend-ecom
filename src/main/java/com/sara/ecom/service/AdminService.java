package com.sara.ecom.service;

import com.sara.ecom.dto.AdminCreateRequest;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminUpdateRequest;
import com.sara.ecom.entity.Admin;
import com.sara.ecom.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public List<AdminDto> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }
    
    public AdminDto getAdminById(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return toAdminDto(admin);
    }
    
    @Transactional
    public AdminDto createAdmin(AdminCreateRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setStatus(Admin.Status.ACTIVE);
        
        admin = adminRepository.save(admin);
        return toAdminDto(admin);
    }
    
    @Transactional
    public AdminDto updateAdmin(Long id, AdminUpdateRequest request) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (request.getName() != null) {
            admin.setName(request.getName());
        }
        if (request.getEmail() != null) {
            admin.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            admin.setStatus(Admin.Status.valueOf(request.getStatus().toUpperCase()));
        }
        
        admin = adminRepository.save(admin);
        return toAdminDto(admin);
    }
    
    @Transactional
    public void deleteAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        // Prevent deleting the default admin
        if ("admin".equals(admin.getUsername())) {
            throw new RuntimeException("Cannot delete default admin account");
        }
        
        adminRepository.delete(admin);
    }
    
    @Transactional
    public AdminDto updateAdminStatus(Long id, String status) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        // Prevent deactivating the default admin
        if ("admin".equals(admin.getUsername()) && "INACTIVE".equals(status.toUpperCase())) {
            throw new RuntimeException("Cannot deactivate default admin account");
        }
        
        admin.setStatus(Admin.Status.valueOf(status.toUpperCase()));
        admin = adminRepository.save(admin);
        return toAdminDto(admin);
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
