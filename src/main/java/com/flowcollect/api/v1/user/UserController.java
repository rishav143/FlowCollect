package com.flowcollect.api.v1.user;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flowcollect.api.v1.user.dto.UserCreateRequest;
import com.flowcollect.api.v1.user.dto.UserPasswordChangeRequest;
import com.flowcollect.api.v1.user.dto.UserResponse;
import com.flowcollect.api.v1.user.dto.UserUpdateRequest;
import com.flowcollect.application.user.UserService;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create User
    @PostMapping
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<UserResponse> create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UserCreateRequest request
    ) {
        User created = userService.create(organizationId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(UserMapper.toResponse(created));
    }

    // Get All Users
    @GetMapping
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<Page<UserResponse>> list(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role,
            Pageable pageable
    ) {
        Page<User> users = userService.list(organizationId, status, email, name, role, pageable);
        return ResponseEntity.ok(users.map(UserMapper::toResponse));
    }

    // Get User by ID
    @GetMapping("/{userId}")
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<UserResponse> getById(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId
    ) {
        User user = userService.getById(organizationId, userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    // Update User
    @PatchMapping("/{userId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        User updated = userService.update(organizationId, userId, request);
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    // Activate User
    @PostMapping("/{userId}/activate")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<UserResponse> activate(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId
    ) {
        User activated = userService.activate(organizationId, userId);
        return ResponseEntity.ok(UserMapper.toResponse(activated));
    }

    // Deactivate User
    @PostMapping("/{userId}/deactivate")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<UserResponse> deactivate(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId
    ) {
        User deactivated = userService.deactivate(organizationId, userId);
        return ResponseEntity.ok(UserMapper.toResponse(deactivated));
    }

    // Change User Password
    @PostMapping("/{userId}/password")
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @Valid @RequestBody UserPasswordChangeRequest request
    ) {
        userService.changePassword(organizationId, userId, request);
        return ResponseEntity.noContent().build();
    }

    // Delete User
    @DeleteMapping("/{userId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<Void> delete(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId
    ) {
        userService.delete(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}
