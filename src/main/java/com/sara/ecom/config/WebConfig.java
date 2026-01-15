package com.sara.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // This configuration ensures that API routes are handled by controllers
    // and not treated as static resources, preventing "No static resource" errors
    // The default Spring Boot behavior already routes /api/** to controllers,
    // but this explicit config ensures proper handling of dynamic paths like /api/categories/monar/konar
}
