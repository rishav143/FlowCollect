package com.cashclarity.api.v1.user;

import com.cashclarity.api.v1.user.dto.UserResponse;
import com.cashclarity.domain.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setOrganizationId(user.getOrganization() != null ? user.getOrganization().getId() : null);
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
