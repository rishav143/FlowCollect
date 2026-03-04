package com.paidpeace.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.paidpeace.exception.code.UserErrorCode;
import com.paidpeace.exception.http.ValidationException;

import java.util.Set;

public class PaginationUtils {
    public static void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Pageable must not be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Page number must be greater than or equal to 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 100) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Page size must be between 1 and 100");
        }

        Set<String> allowedSort = Set.of("id", "name", "email", "role", "status", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                    "Unsupported sort property '" + order.getProperty() + "' for user list");
            }
        }
    }
}
