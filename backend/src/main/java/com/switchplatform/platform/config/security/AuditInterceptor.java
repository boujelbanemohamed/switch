package com.switchplatform.platform.config.security;

import com.switchplatform.platform.service.auth.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditInterceptor {

    private final AuditService auditService;

    @Before("@annotation(auditLog)")
    public void audit(JoinPoint joinPoint, AuditLog auditLog) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "anonymous";
            UUID userId = null;

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String action = auditLog.action().isEmpty()
                    ? signature.getName() : auditLog.action();
            String resourceType = auditLog.resourceType();
            String resourceId = resolveResourceId(joinPoint, auditLog.resourceId());

            HttpServletRequest request = null;
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) request = attrs.getRequest();

            auditService.record(action, resourceType, resourceId,
                    auditLog.details(), "SUCCESS", username, userId, request);
        } catch (Exception e) {
            // Audit should never break business logic
        }
    }

    private String resolveResourceId(JoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank() || expression.startsWith("'")) {
            return expression.replace("'", "");
        }
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(expression) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }
        return expression;
    }
}
