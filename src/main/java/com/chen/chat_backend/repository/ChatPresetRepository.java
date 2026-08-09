package com.chen.chat_backend.repository;

import com.chen.chat_backend.entity.ChatPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Spring generates these database queries from the method names.
public interface ChatPresetRepository extends JpaRepository<ChatPreset, String> {
    List<ChatPreset> findByUserEmailOrderByUpdatedAtDesc(String userEmail);
    Optional<ChatPreset> findByIdAndUserEmail(String id, String userEmail);
}
