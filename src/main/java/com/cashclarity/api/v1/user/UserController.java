package com.cashclarity.api.v1.user;

import com.cashclarity.api.v1.user.dto.UserCreateRequest;
import com.cashclarity.api.v1.user.dto.UserResponse;
import com.cashclarity.application.user.UserService;
import com.cashclarity.domain.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @PathVariable Long organizationId,
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

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @PathVariable Long organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role,
            Pageable pageable
    ) {
        Page<User> users = userService.list(organizationId, status, email, name, role, pageable);
        return ResponseEntity.ok(users.map(UserMapper::toResponse));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(
            @PathVariable Long organizationId,
            @PathVariable Long userId
    ) {
        User user = userService.getById(organizationId, userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }
}
