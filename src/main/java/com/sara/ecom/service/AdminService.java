package com.sara.ecom.service;

import com.sara.ecom.dto.AdminCreateRequest;
import com.sara.ecom.dto.AdminDto;
import com.sara.ecom.dto.AdminUpdateRequest;
import com.sara.ecom.dto.AdminInviteRequest;
import com.sara.ecom.dto.AdminInviteAcceptRequest;
import com.sara.ecom.entity.Admin;
import com.sara.ecom.entity.AdminInvite;
import com.sara.ecom.repository.AdminRepository;
import com.sara.ecom.repository.AdminInviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private AdminInviteRepository adminInviteRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtService jwtService;
    
    @Value("${app.url:https://www.studiosara.in}")
    private String appUrl;
    
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
    
    /**
     * Send admin invitation email
     */
    @Transactional
    public void sendAdminInvite(String email, String invitedByEmail) {
        // Check if admin already exists
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("An admin with this email already exists");
        }
        
        // Check if there's a pending invite for this email
        if (adminInviteRepository.existsByEmailAndStatus(email, AdminInvite.InviteStatus.PENDING)) {
            throw new RuntimeException("An invitation has already been sent to this email");
        }
        
        // Create invite
        AdminInvite invite = new AdminInvite();
        invite.setEmail(email.toLowerCase().trim());
        invite.setInvitedBy(invitedByEmail);
        invite.setStatus(AdminInvite.InviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days expiry
        
        invite = adminInviteRepository.save(invite);
        
        // Send invitation email
        String inviteUrl = appUrl + "/admin-sara/invite/" + invite.getToken();
        sendInviteEmail(email, inviteUrl, invitedByEmail);
    }
    
    /**
     * Accept admin invitation and create admin account
     */
    @Transactional
    public AdminDto acceptAdminInvite(AdminInviteAcceptRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        
        // Find invite by token
        AdminInvite invite = adminInviteRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
        
        // Check if invite is already accepted
        if (invite.getStatus() == AdminInvite.InviteStatus.ACCEPTED) {
            throw new RuntimeException("This invitation has already been accepted");
        }
        
        // Check if invite is expired
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(AdminInvite.InviteStatus.EXPIRED);
            adminInviteRepository.save(invite);
            throw new RuntimeException("This invitation has expired");
        }
        
        // Check if admin already exists
        if (adminRepository.existsByEmail(invite.getEmail())) {
            throw new RuntimeException("An admin with this email already exists");
        }
        
        // Create admin account
        Admin admin = new Admin();
        admin.setEmail(invite.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setName(request.getName());
        
        // Generate username from email
        String baseUsername = invite.getEmail().split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (adminRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        admin.setUsername(username);
        admin.setStatus(Admin.Status.ACTIVE);
        
        admin = adminRepository.save(admin);
        
        // Mark invite as accepted
        invite.setStatus(AdminInvite.InviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        adminInviteRepository.save(invite);
        
        return toAdminDto(admin);
    }
    
    /**
     * Get invite details by token (for invite page)
     */
    public AdminInvite getInviteByToken(String token) {
        AdminInvite invite = adminInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
        
        // Check if expired
        if (invite.getExpiresAt().isBefore(LocalDateTime.now()) && invite.getStatus() == AdminInvite.InviteStatus.PENDING) {
            invite.setStatus(AdminInvite.InviteStatus.EXPIRED);
            adminInviteRepository.save(invite);
        }
        
        return invite;
    }
    
    /**
     * Send invitation email
     */
    private void sendInviteEmail(String toEmail, String inviteUrl, String invitedBy) {
        emailService.sendAdminInviteEmail(toEmail, inviteUrl, invitedBy);
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
