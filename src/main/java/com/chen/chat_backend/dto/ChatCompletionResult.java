package com.chen.chat_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// Internal representation of an OpenAI reply and its token usage.
public class ChatCompletionResult {
    private String reply;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
