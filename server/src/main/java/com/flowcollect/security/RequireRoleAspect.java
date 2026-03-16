package com.flowcollect.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.flowcollect.domain.user.UserRole;
import com.flowcollect.exception.http.ForbiddenException;
import com.flowcollect.exception.http.UnauthorizedException;

/**
 * Enforces @RequireRole on controller methods using the global AuthContext (role from JWT).
 */
@Aspect
@Component
@Order(2)
public class RequireRoleAspect {

    @Before("@annotation(requireRole)")
    public void checkMethodRole(RequireRole requireRole) {
        enforce(requireRole);
    }

    @Before("@within(requireRole)")
    public void checkClassRole(RequireRole requireRole) {
        enforce(requireRole);
    }

    private void enforce(RequireRole requireRole) {
        AuthContext ctx = AuthContext.get();
        if (ctx == null) {
            throw new UnauthorizedException("Authentication required");
        }
        UserRole userRole = ctx.getRole();
        if (userRole == null) {
            throw new ForbiddenException("User role is unknown");
        }
        Set<UserRole> allowed = Arrays.stream(requireRole.value())
                .collect(() -> EnumSet.noneOf(UserRole.class), Set::add, Set::addAll);
        if (!allowed.contains(userRole)) {
            throw new ForbiddenException("Insufficient role. Required one of: " + allowed);
        }
    }
}
