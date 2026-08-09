package com.chen.chat_backend.dto;

import com.chen.chat_backend.enums.AIModel;
import com.chen.chat_backend.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Compact message form serialized into Redis for recent-history caching.
public class ChatMessageCacheEntry {
    private Long id;
    private String role;
    private String content;
    private AIModel model;
    private ChatMessageType messageType;
    private String fileName;
    private String fileKey;
    private LocalDateTime createdAt;
}
