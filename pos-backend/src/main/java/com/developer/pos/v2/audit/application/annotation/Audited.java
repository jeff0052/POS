package com.developer.pos.v2.audit.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit trail recording.
 * The AuditAspect intercepts methods with this annotation and writes
 * before/after snapshots to the audit_trail table.
 *
 * Usage:
 * <pre>
 * {@literal @}Audited(action = "CREATE_USER", targetType = "USER", riskLevel = "MEDIUM")
 * public RbacUserDto createUser(CreateUserRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Action identifier, e.g. "CREATE_USER", "UPDATE_ROLE", "ASSIGN_ROLES" */
    String action();

    /** Target entity type, e.g. "USER", "ROLE", "STORE_ACCESS" */
    String targetType();

    /** SpEL expression to extract target ID from method arguments. Default empty = use return value's id */
    String targetIdExpression() default "";

    /** Risk level: LOW, MEDIUM, HIGH */
    String riskLevel() default "LOW";

    /** If true, creates audit trail with requires_approval=true and approval_status=PENDING */
    boolean requiresApproval() default false;
}
