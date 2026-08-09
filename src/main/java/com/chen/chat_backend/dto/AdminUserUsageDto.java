package com.chen.chat_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
// Usage totals and model breakdown for one user in the admin response.
public class AdminUserUsageDto {
    private String email;
    private String username;
    private long sessionCount;
    private long requestCount;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private LocalDateTime lastUsedAt;
    private List<AdminModelUsageDto> models;
}
