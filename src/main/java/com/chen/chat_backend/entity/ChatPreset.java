package com.chen.chat_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_presets")
@Data
// Database record for one reusable chat configuration.
public class ChatPreset {

    @Id
    private String id;

    // Used for ownership checks, but never returned to the browser.
    @JsonIgnore
    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
