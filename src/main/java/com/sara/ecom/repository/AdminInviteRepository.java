package com.sara.ecom.repository;

import com.sara.ecom.entity.AdminInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {
    
    Optional<AdminInvite> findByToken(String token);
    
    Optional<AdminInvite> findByEmailAndStatus(String email, AdminInvite.InviteStatus status);
    
    boolean existsByEmailAndStatus(String email, AdminInvite.InviteStatus status);
    
    // Find expired invites
    java.util.List<AdminInvite> findByStatusAndExpiresAtBefore(AdminInvite.InviteStatus status, LocalDateTime dateTime);
}
