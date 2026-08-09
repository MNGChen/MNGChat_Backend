package com.chen.chat_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
// Complete payload returned to the protected admin usage page.
public class AdminUsageResponse {
    private long accountCount;
    private long requestCount;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private LocalDateTime generatedAt;
    private List<AdminUserUsageDto> users;
}
