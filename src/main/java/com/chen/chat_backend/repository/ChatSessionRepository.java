package com.chen.chat_backend.repository;

import com.chen.chat_backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Database queries for finding a user's conversations.
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 根据用户获取所有 session（按最近更新时间排序）
    List<ChatSession> findByEmailOrderByUpdatedAtDesc(String email);

    // 根据 sessionId 查 session（前端会用）
    Optional<ChatSession> findBySessionId(String sessionId);
}
