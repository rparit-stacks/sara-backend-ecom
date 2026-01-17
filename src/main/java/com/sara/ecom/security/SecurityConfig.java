package com.sara.ecom.security;

import com.sara.ecom.controller.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // OAuth2 endpoints (must be first to avoid redirect loops)
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                // Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/auth/**").permitAll()
                // Public shipping endpoint (for guest checkout)
                .requestMatchers("/api/shipping/calculate").permitAll()
                // Order endpoints (for guest checkout and confirmation page)
                .requestMatchers("/api/orders").permitAll()
                .requestMatchers("/api/orders/**").permitAll()
                // Admin endpoints - they handle their own token verification
                .requestMatchers("/api/admin/**").permitAll()
                // Public API endpoints
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/plain-products/**").permitAll()
                .requestMatchers("/api/designs/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/cms/**").permitAll()
                .requestMatchers("/api/blogs/**").permitAll()
                .requestMatchers("/api/faqs/**").permitAll()
                .requestMatchers("/api/custom-config").permitAll()
                .requestMatchers("/api/custom-design-requests").permitAll()
                .requestMatchers("/api/testimonials/**").permitAll()
                .requestMatchers("/api/subscribe").permitAll()
                .requestMatchers("/api/instagram/thumbnail").permitAll()
                .requestMatchers("/api/coupons/validate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/custom-products").permitAll() // Allow anyone to create custom products
                .requestMatchers(HttpMethod.DELETE, "/api/custom-products/unsaved/**").permitAll() // Allow deletion of unsaved products (with userEmail param)
                .requestMatchers(HttpMethod.GET, "/api/custom-products/*").permitAll() // Allow access to specific custom products (with userEmail param)
                .requestMatchers("/api/custom-products/**").authenticated() // Other operations require authentication
                .requestMatchers(HttpMethod.POST, "/api/whatsapp/webhook", "/api/whatsapp/webhook/**").permitAll() // Allow webhook from WASender
                // Error handling
                .requestMatchers("/error").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    (request, response, authException) -> {
                        // For API endpoints, return 401 instead of redirecting to OAuth2
                        if (request.getRequestURI().startsWith("/api/")) {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        } else {
                            // For non-API endpoints (like browser requests), allow OAuth2 redirect
                            try {
                                response.sendRedirect("/oauth2/authorization/google");
                            } catch (java.io.IOException e) {
                                response.setStatus(401);
                            }
                        }
                    },
                    AnyRequestMatcher.INSTANCE
                )
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
