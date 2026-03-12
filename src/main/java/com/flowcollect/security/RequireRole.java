package com.flowcollect.security;

import com.flowcollect.domain.user.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pre-auth style annotation: endpoint is allowed only if the current user's role
 * (from AuthContext / JWT) is one of the given roles.
 * Use on controller methods. Throws ForbiddenException if role is not allowed.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    UserRole[] value();
}
