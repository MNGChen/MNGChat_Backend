package com.chen.chat_backend.repository;

import com.chen.chat_backend.dto.AdminModelUsageRow;
import com.chen.chat_backend.dto.AdminUserUsageRow;
import com.chen.chat_backend.entity.ChatMessage;
import com.chen.chat_backend.entity.ChatSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Database queries for messages, including history and usage reporting.
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

    List<ChatMessage> findTop20BySessionOrderByCreatedAtDesc(ChatSession session);

    List<ChatMessage> findBySessionOrderByCreatedAtDesc(ChatSession session, Pageable pageable);

    void  deleteBySession(ChatSession session);

    @Query("""
            select
                m.userEmail as userEmail,
                count(distinct m.session.id) as sessionCount,
                count(m.id) as requestCount,
                coalesce(sum(m.promptTokens), 0) as promptTokens,
                coalesce(sum(m.completionTokens), 0) as completionTokens,
                coalesce(sum(m.totalTokens), 0) as totalTokens,
                max(m.createdAt) as lastUsedAt
            from ChatMessage m
            where m.role = 'assistant' and m.messageType = com.chen.chat_backend.enums.ChatMessageType.TEXT
            group by m.userEmail
            order by coalesce(sum(m.totalTokens), 0) desc, max(m.createdAt) desc
            """)
    List<AdminUserUsageRow> getAdminUsageByUser();

    @Query("""
            select
                m.userEmail as userEmail,
                m.model as model,
                count(m.id) as requestCount,
                coalesce(sum(m.promptTokens), 0) as promptTokens,
                coalesce(sum(m.completionTokens), 0) as completionTokens,
                coalesce(sum(m.totalTokens), 0) as totalTokens
            from ChatMessage m
            where m.role = 'assistant' and m.messageType = com.chen.chat_backend.enums.ChatMessageType.TEXT and m.model is not null
            group by m.userEmail, m.model
            order by m.userEmail asc, coalesce(sum(m.totalTokens), 0) desc
            """)
    List<AdminModelUsageRow> getAdminUsageByUserAndModel();
}
