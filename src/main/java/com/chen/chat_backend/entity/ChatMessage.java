package com.chen.chat_backend.entity;

import com.chen.chat_backend.enums.AIModel;
import com.chen.chat_backend.enums.ChatMessageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
// Persistent record for a text or image message belonging to one chat session.
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_msg_seq")
    @SequenceGenerator(name = "chat_msg_seq", sequenceName = "chat_msg_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "user_email")
    private String userEmail;

    @ManyToOne
    @JoinColumn(name = "session")
    @JsonIgnore
    private ChatSession session;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private AIModel model;

    @Enumerated(EnumType.STRING)
    private ChatMessageType messageType = ChatMessageType.TEXT;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private String fileName;

    private String fileKey;

    private String contentType;

    private LocalDateTime createdAt = LocalDateTime.now();
}
