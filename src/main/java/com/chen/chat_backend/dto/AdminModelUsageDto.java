package com.chen.chat_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// API response item: one model's usage totals for a user.
public class AdminModelUsageDto {
    private String model;
    private long requestCount;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
}
