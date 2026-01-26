package com.sara.ecom.security;

import com.sara.ecom.entity.User;
import com.sara.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Normalize email to lowercase to match how it's stored
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("") // No password, using OTP/OAuth
                .authorities("ROLE_USER")
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
