package com.sara.ecom.security;

import com.sara.ecom.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            
            // Check if this is an admin token - if so, skip regular user authentication
            // Admin tokens are handled separately by AdminAuthController
            if (jwtService.isAdminToken(jwt)) {
                // Admin token - skip regular user authentication
                // Admin endpoints handle their own authentication
                filterChain.doFilter(request, response);
                return;
            }
            
            final String userEmail = jwtService.extractEmail(jwt);
            
            // Always try to set authentication if we have a valid email and token
            // Don't check if authentication is already set - Spring Security will handle that
            if (userEmail != null) {
                try {
                    // Validate token first before loading user details
                    if (!jwtService.validateToken(jwt, userEmail)) {
                        logger.error("JWT Filter: Token validation failed for email: " + userEmail);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Load user details - if user doesn't exist, this will throw UsernameNotFoundException
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    
                    // Create authentication token and set it in SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JWT Filter: Authentication set successfully for: " + userEmail);
                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                    logger.error("JWT Filter: User not found for email: " + userEmail + ". Token is valid but user doesn't exist in database.", e);
                    // Don't set authentication - Spring Security will return 401
                    // This can happen if user was deleted after token was issued
                } catch (Exception e) {
                    logger.error("JWT Filter: Unexpected error loading user details for email: " + userEmail, e);
                    // Don't set authentication - Spring Security will return 401
                }
            } else {
                logger.error("JWT Filter: Could not extract email from token");
            }
        } catch (io.jsonwebtoken.JwtException e) {
            logger.error("JWT Filter: Invalid JWT token - " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("JWT Filter: Cannot set user authentication - " + e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
}
