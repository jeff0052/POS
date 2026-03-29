package com.developer.pos.v2.audit.application.aspect;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.audit.application.annotation.Audited;
import com.developer.pos.v2.audit.infrastructure.persistence.entity.AuditTrailEntity;
import com.developer.pos.v2.audit.infrastructure.persistence.repository.JpaAuditTrailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * AOP aspect that intercepts @Audited methods and writes audit_trail records.
 *
 * Flow:
 * 1. Before: capture method args as "before" context
 * 2. Execute the method
 * 3. After: capture return value as "after" context
 * 4. Write audit_trail record asynchronously (best-effort, never blocks business logic)
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private final JpaAuditTrailRepository auditTrailRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(JpaAuditTrailRepository auditTrailRepository, ObjectMapper objectMapper) {
        this.auditTrailRepository = auditTrailRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        // Capture before state (method arguments as JSON)
        String beforeSnapshot = null;
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                beforeSnapshot = objectMapper.writeValueAsString(args[0]);
            }
        } catch (Exception e) {
            log.debug("Failed to serialize before snapshot: {}", e.getMessage());
        }

        // Execute the actual method
        Object result = joinPoint.proceed();

        // Write audit trail (best-effort, never throw)
        try {
            writeAuditTrail(joinPoint, audited, beforeSnapshot, result);
        } catch (Exception e) {
            log.warn("Failed to write audit trail for action={}: {}", audited.action(), e.getMessage());
        }

        return result;
    }

    private void writeAuditTrail(ProceedingJoinPoint joinPoint, Audited audited,
                                  String beforeSnapshot, Object result) throws Exception {
        // Resolve actor from security context
        String actorType = "HUMAN";
        Long actorId = null;
        String actorName = null;
        Long storeId = 0L;

        try {
            AuthenticatedActor actor = AuthContext.current();
            actorType = "HUMAN";
            actorId = actor.userId();
            actorName = actor.username();
            storeId = actor.storeId() != null ? actor.storeId() : 0L;
        } catch (Exception e) {
            // No auth context (e.g., bootstrap) — use defaults
            actorType = "SYSTEM";
        }

        // Resolve target ID
        String targetId = resolveTargetId(joinPoint, audited, result);

        // Serialize after snapshot
        String afterSnapshot = null;
        if (result != null) {
            try {
                afterSnapshot = objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.debug("Failed to serialize after snapshot: {}", e.getMessage());
            }
        }

        // Get IP address
        String ipAddress = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isBlank()) {
                    ipAddress = request.getRemoteAddr();
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // Build and save
        AuditTrailEntity trail = new AuditTrailEntity();
        trail.setTrailNo("AT-" + UUID.randomUUID().toString().substring(0, 12));
        trail.setStoreId(storeId);
        trail.setActorType(actorType);
        trail.setActorId(actorId);
        trail.setActorName(actorName);
        trail.setAction(audited.action());
        trail.setTargetType(audited.targetType());
        trail.setTargetId(targetId);
        trail.setBeforeSnapshot(beforeSnapshot);
        trail.setAfterSnapshot(afterSnapshot);
        trail.setRiskLevel(audited.riskLevel());
        trail.setRequiresApproval(audited.requiresApproval());
        trail.setIpAddress(ipAddress);

        if (audited.requiresApproval()) {
            trail.setApprovalStatus("PENDING");
        }

        auditTrailRepository.save(trail);
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        // Try SpEL expression first
        if (!audited.targetIdExpression().isEmpty()) {
            try {
                Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
                String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
                Object[] args = joinPoint.getArgs();

                EvaluationContext ctx = new StandardEvaluationContext();
                if (paramNames != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        ((StandardEvaluationContext) ctx).setVariable(paramNames[i], args[i]);
                    }
                }
                ((StandardEvaluationContext) ctx).setVariable("result", result);

                Object val = SPEL_PARSER.parseExpression(audited.targetIdExpression()).getValue(ctx);
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception e) {
                log.debug("SpEL evaluation failed for {}: {}", audited.targetIdExpression(), e.getMessage());
            }
        }

        // Try extracting "id" from result via reflection
        if (result != null) {
            try {
                var idMethod = result.getClass().getMethod("id");
                Object id = idMethod.invoke(result);
                if (id != null) return id.toString();
            } catch (NoSuchMethodException e) {
                // try getId()
                try {
                    var getIdMethod = result.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(result);
                    if (id != null) return id.toString();
                } catch (Exception ex) {
                    // ignore
                }
            } catch (Exception e) {
                // ignore
            }
        }

        // Fallback: first argument if it's a Long (likely an ID)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            return args[0].toString();
        }

        return "unknown";
    }
}
