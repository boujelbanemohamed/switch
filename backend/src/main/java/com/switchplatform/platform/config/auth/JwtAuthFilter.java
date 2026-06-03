package com.switchplatform.platform.config.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.service.auth.AuthUserService;
import com.switchplatform.platform.service.auth.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthUserService authUserService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            if (tokenBlacklistService.isRevoked(token)) {
                log.warn("Revoked token used: {}", token.substring(0, 20));
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                UserDetails userDetails = authUserService.loadUserByUsername(username);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    AuthUser authUser = authUserService.findByUsername(username);
                    if (authUser != null && authUser.isMustChangePassword()
                            && !request.getRequestURI().equals("/api/v1/auth/change-password")
                            && !request.getRequestURI().equals("/api/v1/auth/logout")
                            && !request.getRequestURI().equals("/api/v1/auth/me")) {
                        response.setStatus(426);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Password change required\",\"mustChangePassword\":true}");
                        response.getWriter().flush();
                        return;
                    }

                    log.debug("Authenticated user: {}", username);
                }
            } catch (Exception e) {
                log.warn("Failed to authenticate user from token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
