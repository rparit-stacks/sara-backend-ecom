package com.sara.ecom.repository;

import com.sara.ecom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByOauthProviderId(String oauthProviderId);
    
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByStatusOrderByCreatedAtDesc(User.UserStatus status);
    long countByStatus(User.UserStatus status);
}
