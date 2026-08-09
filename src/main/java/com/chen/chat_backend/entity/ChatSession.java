package com.chen.chat_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
// Persistent conversation metadata; messages are linked through the session relationship.
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_session_seq")
    @SequenceGenerator(name = "chat_session_seq", sequenceName = "chat_session_sequence", allocationSize = 1)
    private Long id;

    // 用户（和 ChatMessage 对应）
    private String email;

    // 前端使用的 sessionId（UUID）
    @Column(unique = true, nullable = false)
    private String sessionId;

    // 会话标题（比如：New Chat / 用户第一句话）
    private String title;



    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间（用于排序最近聊天）
    private LocalDateTime updatedAt;

    // 👉 一个 session 对应多个 message
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> messages;
}
