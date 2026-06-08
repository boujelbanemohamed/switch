package com.switchplatform.platform.config.auth;

import com.switchplatform.platform.config.ratelimit.RateLimitingFilter;
import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.service.auth.AuthUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AuthUserService authUserService;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(authUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isProd = environment.matchesProfiles("prod");
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (!isProd) {
                    auth.requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll();
                }
                auth
                .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/auth/login"),
                        new AntPathRequestMatcher("/api/v1/auth/refresh"),
                        new AntPathRequestMatcher("/api/v1/auth/mfa/authenticate")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/register")).hasAnyRole(AuthUser.Role.ADMIN.name(), AuthUser.Role.OPERATOR.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/auth/mfa/setup"),
                        new AntPathRequestMatcher("/api/v1/auth/mfa/verify"),
                        new AntPathRequestMatcher("/api/v1/auth/mfa/disable")).hasAnyRole(AuthUser.Role.ADMIN.name(), AuthUser.Role.OPERATOR.name(), AuthUser.Role.ANALYST.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/auth/users/**"),
                        new AntPathRequestMatcher("/api/v1/auth/audit/**")).hasRole(AuthUser.Role.ADMIN.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/me")).hasAnyRole(AuthUser.Role.ADMIN.name(), AuthUser.Role.OPERATOR.name(), AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/backoffice/monitoring/events")).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher("/actuator/health"),
                        new AntPathRequestMatcher("/actuator/info")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).hasRole(AuthUser.Role.ADMIN.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/acs/**"),
                        new AntPathRequestMatcher("/api/v1/epg/**"),
                        new AntPathRequestMatcher("/api/v1/3dss/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/merchant-portal/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name(),
                        AuthUser.Role.MERCHANT.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/merchant-portal/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.MERCHANT.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/issuing/virtual-cards/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/admin/live-config/**")).hasRole(AuthUser.Role.ADMIN.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/admin/**")).hasRole(AuthUser.Role.ADMIN.name())

                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/fraud/alerts/**", "GET"),
                        new AntPathRequestMatcher("/api/v1/fraud/rules", "GET"),
                        new AntPathRequestMatcher("/api/v1/fraud/profiles/**", "GET"),
                        new AntPathRequestMatcher("/api/v1/fraud/devices/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/fraud/devices/register"),
                        new AntPathRequestMatcher("/api/v1/fraud/devices/evaluate")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/fraud/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/backoffice/audit/**", "GET"),
                        new AntPathRequestMatcher("/api/v1/backoffice/reports", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.AUDITOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/backoffice/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers(new AntPathRequestMatcher("/api/v1/clearing/interchange/configure")).hasRole(AuthUser.Role.ADMIN.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/clearing/interchange", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/clearing/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/clearing/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/issuing/pins/**"),
                        new AntPathRequestMatcher("/api/v1/issuing/tokens/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/issuing/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers(new AntPathRequestMatcher("/api/v1/authorization/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/acquiring/settlements/**"),
                        new AntPathRequestMatcher("/api/v1/acquiring/terminals/**"),
                        new AntPathRequestMatcher("/api/v1/acquiring/merchants/*/netting")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/switch/mq/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())

                .requestMatchers(new AntPathRequestMatcher("/api/v1/disputes/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())

                .requestMatchers(new AntPathRequestMatcher("/api/v1/standin/pending/count", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/standin/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/standin/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/credit/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/credit/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/loyalty/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/loyalty/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/batch/**")).hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/netting/**")).hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/fees/**")).hasAnyRole("ADMIN", "OPERATOR", "ANALYST")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/issuing/programs/**")).hasAnyRole(
                        "ADMIN", "OPERATOR", "ANALYST")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/kyc/**")).hasAnyRole(
                        "ADMIN", "OPERATOR", "ANALYST")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/ecommerce/cof/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/ecommerce/cof/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/fx/rates", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/fx/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/regulatory/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/transfers/**", "GET")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name())
                .requestMatchers(new AntPathRequestMatcher("/api/v1/transfers/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name())
                .requestMatchers(new AntPathRequestMatcher("/**")).hasAnyRole(
                        AuthUser.Role.ADMIN.name(),
                        AuthUser.Role.OPERATOR.name(),
                        AuthUser.Role.ANALYST.name(),
                        AuthUser.Role.MERCHANT.name(),
                        AuthUser.Role.AUDITOR.name(),
                        AuthUser.Role.VIEWER.name());
            })
            .authenticationProvider(authenticationProvider())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Forbidden\"}");
                })
            )
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(f -> f.deny())
                .xssProtection(x -> x.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(h -> h
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .cacheControl(Customizer.withDefaults()))
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
