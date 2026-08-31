package com.powercity.power_city_platform.dto.request.user;

public record UserSearchRequest(
        String searchTerm,
        String fullName,
        String email,
        Boolean enabled,
        Boolean emailVerified,
        Boolean accountLocked,
        String role,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
    public UserSearchRequest {
        if (page < 0) page = 0;
        if (size < 1 || size > 500) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc"))) {
            sortDirection = "desc";
        }
    }
}
