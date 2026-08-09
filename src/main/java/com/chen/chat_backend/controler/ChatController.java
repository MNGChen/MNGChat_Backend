package com.chen.chat_backend.controler;

import com.chen.chat_backend.dto.AdminUsageResponse;
import com.chen.chat_backend.dto.ChatMessageResponse;
import com.chen.chat_backend.entity.ChatSession;
import com.chen.chat_backend.entity.ChatPreset;
import com.chen.chat_backend.enums.AIModel;
import com.chen.chat_backend.request.ChatRequest;
import com.chen.chat_backend.request.PresetRequest;
import com.chen.chat_backend.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {
    @Autowired
    private ChatService chatService;

    // HTTP endpoints stay thin; ChatService handles validation, ownership, and storage.
    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest
    ) {
        return chatService.handleChat(request, httpRequest);
    }

    @GetMapping("/chat/sessions")
    public List<ChatSession> getSessions(HttpServletRequest request) {
        return chatService.getUserSessions(request);
    }

    @PostMapping("/chat/session")
    public ChatSession createSession(HttpServletRequest request) {
        return chatService.createSession(request);
    }

    @GetMapping("/chat/models")
    public List<AIModel> getModels() {
        return List.of(AIModel.values());
    }

    // Presets are private to the logged-in user, identified from the JWT token.
    @GetMapping("/chat/presets")
    public List<ChatPreset> getPresets(HttpServletRequest request) {
        return chatService.getUserPresets(request);
    }

    @PostMapping("/chat/presets")
    public ResponseEntity<ChatPreset> createPreset(
            @RequestBody PresetRequest presetRequest,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(201).body(chatService.createPreset(presetRequest, request));
    }

    @DeleteMapping("/chat/presets/{presetId}")
    public ResponseEntity<Void> deletePreset(
            @PathVariable String presetId,
            HttpServletRequest request
    ) {
        chatService.deletePreset(presetId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chat/admin/usage")
    public AdminUsageResponse getAdminUsage(HttpServletRequest request) {
        return chatService.getAdminUsage(request);
    }

    @GetMapping("/chat/history")
    public List<ChatMessageResponse> getHistory(
            @RequestParam String sessionId,
            HttpServletRequest request
    ) {
        return chatService.getChatHistory(sessionId, request);
    }

    @PostMapping(value = "/chat/session/{sessionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessageResponse uploadImage(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        return chatService.uploadImage(sessionId, file, request);
    }

    @PatchMapping("/chat/session/{sessionId}/title")
    public ResponseEntity<?> updateTitle(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        chatService.updateSessionTitle(sessionId, body.get("title"), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        chatService.deleteSession(sessionId, request);
        return ResponseEntity.noContent().build();
    }
}
