package dev.tsvinc.r2p.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R2PApiResponse<T> {
    private T data;
    private ResponseMetadata metadata;
    private PaginationInfo pagination;

    @Data
    @Builder
    public static class ResponseMetadata {
        private String requestId;
        private String correlationId;
        private String timestamp;
        private String apiVersion;
        private String processingTime;
        private Map<String, String> headers;
    }

    @Data
    @Builder
    public static class PaginationInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}