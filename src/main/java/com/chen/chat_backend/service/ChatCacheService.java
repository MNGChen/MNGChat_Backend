package com.chen.chat_backend.service;

import com.chen.chat_backend.dto.ChatMessageCacheEntry;
import com.chen.chat_backend.entity.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
// Keeps a bounded recent-message list in Redis to reduce repeated database reads.
public class ChatCacheService {

    private static final String KEY_PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;
    private final Duration ttl;

    public ChatCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${chat.cache.max-messages:50}") int maxMessages,
            @Value("${chat.cache.ttl-days:7}") long ttlDays
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxMessages = Math.max(1, maxMessages);
        this.ttl = Duration.ofDays(Math.max(1, ttlDays));
    }

    // Cache failures are treated as misses so chatting can continue using the database.
    public Optional<List<ChatMessageCacheEntry>> getRecentMessages(String sessionId) {
        try {
            List<String> cachedMessages = redisTemplate.opsForList().range(key(sessionId), 0, -1);
            if (cachedMessages == null || cachedMessages.isEmpty()) {
                return Optional.empty();
            }

            List<ChatMessageCacheEntry> messages = new ArrayList<>();
            for (String cachedMessage : cachedMessages) {
                messages.add(objectMapper.readValue(cachedMessage, ChatMessageCacheEntry.class));
            }

            return Optional.of(messages);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public void replaceRecentMessages(String sessionId, List<ChatMessage> messages) {
        try {
            String key = key(sessionId);
            redisTemplate.delete(key);

            List<ChatMessage> recentMessages = latestMessages(messages);
            if (recentMessages.isEmpty()) {
                return;
            }

            List<String> serializedMessages = new ArrayList<>();
            for (ChatMessage message : recentMessages) {
                serializedMessages.add(serialize(toCacheEntry(message)));
            }

            redisTemplate.opsForList().rightPushAll(key, serializedMessages);
            redisTemplate.expire(key, ttl);
        } catch (Exception ignored) {
        }
    }

    public void appendMessage(String sessionId, ChatMessage message) {
        try {
            String key = key(sessionId);
            redisTemplate.opsForList().rightPush(key, serialize(toCacheEntry(message)));
            redisTemplate.opsForList().trim(key, -maxMessages, -1);
            redisTemplate.expire(key, ttl);
        } catch (Exception ignored) {
        }
    }

    public void evictSession(String sessionId) {
        try {
            redisTemplate.delete(key(sessionId));
        } catch (Exception ignored) {
        }
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    private List<ChatMessage> latestMessages(List<ChatMessage> messages) {
        if (messages.size() <= maxMessages) {
            return messages;
        }

        return messages.subList(messages.size() - maxMessages, messages.size());
    }

    private ChatMessageCacheEntry toCacheEntry(ChatMessage message) {
        return ChatMessageCacheEntry.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .model(message.getModel())
                .messageType(message.getMessageType())
                .fileName(message.getFileName())
                .fileKey(message.getFileKey())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String serialize(ChatMessageCacheEntry message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
