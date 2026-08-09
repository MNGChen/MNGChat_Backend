package com.chen.chat_backend.request;

import com.chen.chat_backend.enums.AIModel;
import lombok.Data;

@Data

// Payload sent by the frontend when the user submits a chat message.
public class ChatRequest {
    private String message;
    private String sessionId;
    private AIModel model;
    private String systemPrompt;
    private Double temperature;
}
