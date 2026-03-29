package com.developer.pos.v2.audit.application.aspect;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.audit.application.annotation.Audited;
import com.developer.pos.v2.audit.application.event.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * AOP aspect that intercepts @Audited methods and publishes AuditEvent.
 *
 * Key design:
 * - Runs OUTSIDE the business transaction (@Order(Ordered.LOWEST_PRECEDENCE))
 * - Never blocks: publishes an event, actual DB write is async via AuditEventListener
 * - Sanitizes sensitive fields (password, pin, secret, token) before serialization
 * - All context (actor, IP) resolved in request thread before event publish
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /** Fields to redact from audit snapshots */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordHash", "password_hash",
            "pin", "pinHash", "pin_hash",
            "newPassword", "newPin",
            "secret", "token", "accessToken", "refreshToken",
            "creditCard", "cardNumber", "cvv"
    );

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AuditAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        // Capture before state (sanitized)
        String beforeSnapshot = sanitizeAndSerialize(joinPoint.getArgs());

        // Execute the actual method
        Object result = joinPoint.proceed();

        // Publish audit event (non-blocking)
        try {
            publishAuditEvent(joinPoint, audited, beforeSnapshot, result);
        } catch (Exception e) {
            log.warn("Failed to publish audit event for action={}: {}", audited.action(), e.getMessage());
        }

        return result;
    }

    private void publishAuditEvent(ProceedingJoinPoint joinPoint, Audited audited,
                                    String beforeSnapshot, Object result) {
        // Resolve actor from security context (must be done in request thread)
        String actorType = "SYSTEM";
        Long actorId = null;
        String actorName = null;
        Long storeId = null;

        try {
            AuthenticatedActor actor = AuthContext.current();
            actorType = "HUMAN";
            actorId = actor.userId();
            actorName = actor.username();
            storeId = actor.storeId(); // null for merchant-level ops, OK since V104 made store_id nullable
        } catch (Exception e) {
            // No auth context (e.g., bootstrap)
        }

        // Resolve target ID
        String targetId = resolveTargetId(joinPoint, audited, result);

        // Serialize after snapshot (sanitized)
        String afterSnapshot = null;
        if (result != null) {
            afterSnapshot = sanitizeAndSerialize(new Object[]{result});
        }

        // Get IP address (must be done in request thread)
        String ipAddress = resolveIpAddress();

        // Publish event — will be consumed async after transaction commits
        eventPublisher.publishEvent(new AuditEvent(
                "AT-" + UUID.randomUUID().toString().substring(0, 12),
                storeId,
                actorType,
                actorId,
                actorName,
                audited.action(),
                audited.targetType(),
                targetId,
                beforeSnapshot,
                afterSnapshot,
                audited.riskLevel(),
                audited.requiresApproval(),
                ipAddress
        ));
    }

    /**
     * Serialize object to JSON, redacting sensitive fields.
     */
    private String sanitizeAndSerialize(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            Object target = args[0];
            // Convert to JSON tree, redact sensitive fields, then serialize
            ObjectNode node = objectMapper.valueToTree(target);
            for (String field : SENSITIVE_FIELDS) {
                if (node.has(field)) {
                    node.put(field, "[REDACTED]");
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.debug("Failed to serialize snapshot: {}", e.getMessage());
            return null;
        }
    }

    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        if (!audited.targetIdExpression().isEmpty()) {
            try {
                String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
                Object[] args = joinPoint.getArgs();
                StandardEvaluationContext ctx = new StandardEvaluationContext();
                if (paramNames != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        ctx.setVariable(paramNames[i], args[i]);
                    }
                }
                ctx.setVariable("result", result);
                Object val = SPEL_PARSER.parseExpression(audited.targetIdExpression()).getValue(ctx);
                if (val != null) return val.toString();
            } catch (Exception e) {
                log.debug("SpEL evaluation failed: {}", e.getMessage());
            }
        }

        // Try extracting id from result
        if (result != null) {
            try {
                var m = result.getClass().getMethod("id");
                Object id = m.invoke(result);
                if (id != null) return id.toString();
            } catch (NoSuchMethodException e) {
                try {
                    var m = result.getClass().getMethod("getId");
                    Object id = m.invoke(result);
                    if (id != null) return id.toString();
                } catch (Exception ex) { /* ignore */ }
            } catch (Exception e) { /* ignore */ }
        }

        // Fallback: first Long argument
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            return args[0].toString();
        }
        return "unknown";
    }
}
