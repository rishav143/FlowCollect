package com.cashclarity.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import com.cashclarity.exception.user.InvalidUserFieldException;

public class PaginationUtils {
    public static void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidUserFieldException("pageable must not be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new InvalidUserFieldException("page number must be greater than or equal to 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 100) {
            throw new InvalidUserFieldException("page size must be between 1 and 100");
        }

        Set<String> allowedSort = Set.of("id", "name", "email", "role", "status", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new InvalidUserFieldException("unsupported sort property '" + order.getProperty() + "'");
            }
        }
    }
}
