package com.switchplatform.platform.config.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.service.auth.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final JwtAuthFilter jwtAuthFilter;
    private final AuthUserService authUserService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(authUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/backoffice/monitoring/events").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole(AuthUser.Role.ADMIN.name())

                .requestMatchers(HttpMethod.GET, "/api/v1/fraud/alerts/**",
                        "/api/v1/fraud/rules", "/api/v1/fraud/profiles/**").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers("/api/v1/fraud/**").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers(HttpMethod.GET, "/api/v1/backoffice/audit/**",
                        "/api/v1/backoffice/reports").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.AUDITOR.name())
                .requestMatchers("/api/v1/backoffice/**").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers("/api/v1/clearing/**").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers("/api/v1/issuing/**").hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
