package com.flowcollect.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.flowcollect.exception.http.ValidationException;

import java.util.Set;

public class PaginationUtils {
    public static void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new ValidationException( 
                "Pageable must not be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new ValidationException( 
                "Page number must be greater than or equal to 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 100) {
            throw new ValidationException( 
                "Page size must be between 1 and 100");
        }

        Set<String> allowedSort = Set.of("id", "name", "email", "role", "status", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new ValidationException( 
                    "Unsupported sort property '" + order.getProperty() + "' for user list");
            }
        }
    }
}
