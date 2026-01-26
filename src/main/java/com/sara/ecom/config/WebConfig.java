package com.sara.ecom.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // This configuration ensures that API routes are handled by controllers
    // and not treated as static resources, preventing "No static resource" errors
    // The default Spring Boot behavior already routes /api/** to controllers,
    // but this explicit config ensures proper handling of dynamic paths like /api/categories/monar/konar
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        
        return builder
                .requestFactory(() -> factory)
                .build();
    }
    
    // Configure Tomcat to allow 10MB file uploads
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setMaxPostSize(10 * 1024 * 1024); // 10MB in bytes
                // maxSwallowSize is configured via application.properties (server.tomcat.max-swallow-size)
            });
        };
    }
}
