package com.switchplatform.platform.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    public StaticHeadersWriter securityHeadersWriter() {
        return new StaticHeadersWriter(
                "X-Content-Type-Options", "nosniff",
                "X-Frame-Options", "DENY",
                "X-XSS-Protection", "1; mode=block",
                "Strict-Transport-Security", "max-age=31536000; includeSubDomains",
                "Cache-Control", "no-store, no-cache, must-revalidate",
                "Pragma", "no-cache"
        );
    }
}
