package com.chen.chat_backend.dto;

import com.chen.chat_backend.enums.AIModel;
import com.chen.chat_backend.enums.ChatMessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
// Safe message shape returned by chat history and image-upload endpoints.
public class ChatMessageResponse {
    private Long id;
    private String role;
    private String content;
    private AIModel model;
    private ChatMessageType messageType;
    private String fileName;
    private String fileUrl;
    private LocalDateTime createdAt;
}
