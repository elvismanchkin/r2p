package dev.tsvinc.r2p.api.dto.response;

public record Pagination(
        int page,
        int size,
        int totalElements,
        int totalPages
) {
} 