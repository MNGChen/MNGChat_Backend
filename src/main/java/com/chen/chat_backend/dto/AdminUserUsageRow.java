package com.chen.chat_backend.dto;

import java.time.LocalDateTime;

// Projection used by the database aggregation query for per-user usage.
public interface AdminUserUsageRow {
    String getUserEmail();

    long getSessionCount();

    long getRequestCount();

    long getPromptTokens();

    long getCompletionTokens();

    long getTotalTokens();

    LocalDateTime getLastUsedAt();
}
