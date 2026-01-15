package com.sara.ecom.controller;

import com.sara.ecom.dto.AuthResponse;
import com.sara.ecom.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final AuthService authService;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String email = (String) attributes.get("email");
        String providerId = (String) attributes.get("sub");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        
        AuthResponse authResponse = authService.handleOAuthLogin(email, providerId, firstName, lastName);
        
        // Redirect to frontend with token
        // Default frontend URL is http://localhost:3000, can be overridden with FRONTEND_BASE_URL env var
        String frontendBaseUrl = System.getenv().getOrDefault("FRONTEND_BASE_URL", "http://localhost:3000");
        if (frontendBaseUrl.endsWith("/")) {
            frontendBaseUrl = frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1);
        }
        String redirectUrl = String.format(
            "%s/auth/callback?token=%s&email=%s",
            frontendBaseUrl,
            authResponse.getToken(),
            authResponse.getEmail()
        );
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
