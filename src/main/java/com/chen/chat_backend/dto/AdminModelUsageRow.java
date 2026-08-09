package com.chen.chat_backend.dto;

import com.chen.chat_backend.enums.AIModel;

// Projection used by the database aggregation query for model usage.
public interface AdminModelUsageRow {
    String getUserEmail();

    AIModel getModel();

    long getRequestCount();

    long getPromptTokens();

    long getCompletionTokens();

    long getTotalTokens();
}
