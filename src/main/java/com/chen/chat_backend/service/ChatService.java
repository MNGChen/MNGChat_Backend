package com.chen.chat_backend.service;

import com.chen.chat_backend.dto.AdminModelUsageDto;
import com.chen.chat_backend.dto.AdminModelUsageRow;
import com.chen.chat_backend.dto.AdminUsageResponse;
import com.chen.chat_backend.dto.AdminUserUsageDto;
import com.chen.chat_backend.dto.AdminUserUsageRow;
import com.chen.chat_backend.dto.ChatCompletionResult;
import com.chen.chat_backend.dto.ChatMessageCacheEntry;
import com.chen.chat_backend.dto.ChatMessageResponse;
import com.chen.chat_backend.entity.ChatMessage;
import com.chen.chat_backend.entity.ChatPreset;
import com.chen.chat_backend.entity.ChatSession;
import com.chen.chat_backend.entity.UserEntity;
import com.chen.chat_backend.enums.AIModel;
import com.chen.chat_backend.enums.ChatMessageType;
import com.chen.chat_backend.repository.ChatMessageRepository;
import com.chen.chat_backend.repository.ChatPresetRepository;
import com.chen.chat_backend.repository.ChatSessionRepository;
import com.chen.chat_backend.repository.UserRepository;
import com.chen.chat_backend.request.ChatRequest;
import com.chen.chat_backend.request.PresetRequest;
import com.chen.chat_backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    // Central chat workflow: authorization, persistence, cache access, and OpenAI requests.

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatPresetRepository presetRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private ChatCacheService chatCacheService;

    private final String openaiKey = System.getenv("OPENAI_API_KEY");
    private final String adminEmails = System.getenv("CHAT_ADMIN_EMAILS");

    // Build the model context from recent history, call OpenAI, then save both messages.
    public Map<String, String> handleChat(ChatRequest request, HttpServletRequest httpRequest) {
        String userEmail = requireUserEmail(httpRequest);
        String sessionId = request.getSessionId();

        if (sessionId == null || sessionId.isEmpty()) {
            throw new RuntimeException("SessionId is required");
        }

        ChatSession session = requireOwnedSession(sessionId, userEmail);
        List<ChatMessageCacheEntry> history = getRecentHistory(session);
        List<Map<String, String>> messages = new ArrayList<>();

        for (ChatMessageCacheEntry msg : history) {
            if (msg.getMessageType() == ChatMessageType.IMAGE) {
                messages.add(Map.of("role", msg.getRole(), "content", "[Image uploaded: " + msg.getFileName() + "]"));
                continue;
            }

            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        // The system prompt comes before chat history so it guides the entire response.
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(0, Map.of("role", "system", "content", request.getSystemPrompt().trim()));
        }

        messages.add(Map.of("role", "user", "content", request.getMessage()));

        AIModel selectedModel = request.getModel() == null ? AIModel.defaultModel() : request.getModel();
        ChatCompletionResult completionResult = callOpenAI(messages, selectedModel, normalizeTemperature(request.getTemperature()));

        saveTextMessage(userEmail, session, "user", request.getMessage(), selectedModel, null, null, null);
        saveTextMessage(
                userEmail,
                session,
                "assistant",
                completionResult.getReply(),
                selectedModel,
                completionResult.getPromptTokens(),
                completionResult.getCompletionTokens(),
                completionResult.getTotalTokens()
        );

        return Map.of(
                "reply", completionResult.getReply(),
                "model", selectedModel.getApiName(),
                "promptTokens", String.valueOf(completionResult.getPromptTokens()),
                "completionTokens", String.valueOf(completionResult.getCompletionTokens()),
                "totalTokens", String.valueOf(completionResult.getTotalTokens())
        );
    }

    public List<ChatSession> getUserSessions(HttpServletRequest request) {
        String email = requireUserEmail(request);
        return sessionRepository.findByEmailOrderByUpdatedAtDesc(email);
    }

    // Every preset query is scoped by email to avoid exposing another user's settings.
    public List<ChatPreset> getUserPresets(HttpServletRequest request) {
        return presetRepository.findByUserEmailOrderByUpdatedAtDesc(requireUserEmail(request));
    }

    // Save a reusable set of chat controls for the current user.
    public ChatPreset createPreset(PresetRequest request, HttpServletRequest httpRequest) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Preset name must be between 1 and 100 characters");
        }

        ChatPreset preset = new ChatPreset();
        preset.setId(UUID.randomUUID().toString());
        preset.setUserEmail(requireUserEmail(httpRequest));
        preset.setName(name);
        preset.setSystemPrompt(request.getSystemPrompt() == null ? "" : request.getSystemPrompt().trim());
        preset.setTemperature(normalizeTemperature(request.getTemperature()));
        preset.setCreatedAt(LocalDateTime.now());
        preset.setUpdatedAt(LocalDateTime.now());
        return presetRepository.save(preset);
    }

    // Look up by both id and owner before deleting.
    public void deletePreset(String presetId, HttpServletRequest request) {
        String email = requireUserEmail(request);
        ChatPreset preset = presetRepository.findByIdAndUserEmail(presetId, email)
                .orElseThrow(() -> new RuntimeException("Preset not found"));
        presetRepository.delete(preset);
    }

    public List<ChatMessageResponse> getChatHistory(String sessionId, HttpServletRequest request) {
        String email = requireUserEmail(request);
        ChatSession session = requireOwnedSession(sessionId, email);

        return getRecentHistory(session)
                .stream()
                .map(this::toChatMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse uploadImage(String sessionId, MultipartFile file, HttpServletRequest request) {
        String email = requireUserEmail(request);
        ChatSession session = requireOwnedSession(sessionId, email);

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image upload is supported");
        }

        S3StorageService.StoredObject storedObject = s3StorageService.uploadImage(file, email, sessionId);

        ChatMessage message = new ChatMessage();
        message.setUserEmail(email);
        message.setSession(session);
        message.setRole("user");
        message.setContent(storedObject.fileName());
        message.setMessageType(ChatMessageType.IMAGE);
        message.setFileName(storedObject.fileName());
        message.setFileKey(storedObject.key());
        message.setContentType(storedObject.contentType());

        ChatMessage saved = messageRepository.save(message);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        chatCacheService.appendMessage(sessionId, saved);

        return toChatMessageResponse(saved);
    }

    public ChatSession createSession(HttpServletRequest request) {
        String email = requireUserEmail(request);

        ChatSession session = new ChatSession();
        session.setEmail(email);
        session.setSessionId(UUID.randomUUID().toString());
        session.setTitle("New Chat");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public void updateSessionTitle(String sessionId, String title, HttpServletRequest request) {
        String email = requireUserEmail(request);
        ChatSession session = requireOwnedSession(sessionId, email);

        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(String sessionId, HttpServletRequest request) {
        String email = requireUserEmail(request);
        ChatSession session = requireOwnedSession(sessionId, email);
        List<ChatMessage> messages = messageRepository.findBySessionOrderByCreatedAtAsc(session);

        for (ChatMessage message : messages) {
            if (message.getMessageType() == ChatMessageType.IMAGE && message.getFileKey() != null) {
                s3StorageService.deleteObject(message.getFileKey());
            }
        }

        messageRepository.deleteBySession(session);
        sessionRepository.delete(session);
        chatCacheService.evictSession(sessionId);
    }

    public AdminUsageResponse getAdminUsage(HttpServletRequest request) {
        requireAdmin(request);

        List<AdminUserUsageRow> userRows = messageRepository.getAdminUsageByUser();
        List<AdminModelUsageRow> modelRows = messageRepository.getAdminUsageByUserAndModel();
        Map<String, String> usernamesByEmail = userRepository.findAll()
                .stream()
                .collect(Collectors.toMap(UserEntity::getEmail, UserEntity::getUsername, (left, right) -> left));
        Map<String, List<AdminModelUsageDto>> modelsByEmail = modelRows.stream()
                .collect(Collectors.groupingBy(
                        AdminModelUsageRow::getUserEmail,
                        Collectors.mapping(row -> AdminModelUsageDto.builder()
                                .model(row.getModel().getApiName())
                                .requestCount(row.getRequestCount())
                                .promptTokens(row.getPromptTokens())
                                .completionTokens(row.getCompletionTokens())
                                .totalTokens(row.getTotalTokens())
                                .build(), Collectors.toList())
                ));

        List<AdminUserUsageDto> users = userRows.stream()
                .map(row -> AdminUserUsageDto.builder()
                        .email(row.getUserEmail())
                        .username(usernamesByEmail.get(row.getUserEmail()))
                        .sessionCount(row.getSessionCount())
                        .requestCount(row.getRequestCount())
                        .promptTokens(row.getPromptTokens())
                        .completionTokens(row.getCompletionTokens())
                        .totalTokens(row.getTotalTokens())
                        .lastUsedAt(row.getLastUsedAt())
                        .models(modelsByEmail.getOrDefault(row.getUserEmail(), List.of()))
                        .build())
                .toList();

        return AdminUsageResponse.builder()
                .accountCount(users.size())
                .requestCount(users.stream().mapToLong(AdminUserUsageDto::getRequestCount).sum())
                .promptTokens(users.stream().mapToLong(AdminUserUsageDto::getPromptTokens).sum())
                .completionTokens(users.stream().mapToLong(AdminUserUsageDto::getCompletionTokens).sum())
                .totalTokens(users.stream().mapToLong(AdminUserUsageDto::getTotalTokens).sum())
                .generatedAt(LocalDateTime.now())
                .users(users)
                .build();
    }

    // Translate the internal chat context into the request expected by OpenAI.
    private ChatCompletionResult callOpenAI(List<Map<String, String>> messages, AIModel model, double temperature) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getApiName());
        body.put("messages", messages);
        body.put("temperature", temperature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response =
                restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", entity, Map.class);

        Map responseBody = response.getBody();
        List choices = (List) responseBody.get("choices");
        Map firstChoice = (Map) choices.get(0);
        Map messageMap = (Map) firstChoice.get("message");
        Map usageMap = (Map) responseBody.get("usage");

        return ChatCompletionResult.builder()
                .reply(messageMap.get("content").toString())
                .promptTokens(readUsageValue(usageMap, "prompt_tokens"))
                .completionTokens(readUsageValue(usageMap, "completion_tokens"))
                .totalTokens(readUsageValue(usageMap, "total_tokens"))
                .build();
    }

    private int readUsageValue(Map usageMap, String key) {
        if (usageMap == null) {
            return 0;
        }

        Object value = usageMap.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    // Keep model creativity within the range supported by the UI and API.
    private double normalizeTemperature(Double temperature) {
        if (temperature == null) {
            return 0.7;
        }
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
        return temperature;
    }

    private void saveTextMessage(
            String userEmail,
            ChatSession session,
            String role,
            String content,
            AIModel model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        ChatMessage message = new ChatMessage();
        message.setUserEmail(userEmail);
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setModel(model);
        message.setMessageType(ChatMessageType.TEXT);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(totalTokens);
        ChatMessage saved = messageRepository.save(message);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        chatCacheService.appendMessage(session.getSessionId(), saved);
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        String fileUrl = null;
        if (message.getMessageType() == ChatMessageType.IMAGE && message.getFileKey() != null) {
            fileUrl = s3StorageService.generatePresignedDownloadUrl(message.getFileKey());
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .model(message.getModel())
                .messageType(message.getMessageType())
                .fileName(message.getFileName())
                .fileUrl(fileUrl)
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessageCacheEntry message) {
        String fileUrl = null;
        if (message.getMessageType() == ChatMessageType.IMAGE && message.getFileKey() != null) {
            fileUrl = s3StorageService.generatePresignedDownloadUrl(message.getFileKey());
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .model(message.getModel())
                .messageType(message.getMessageType())
                .fileName(message.getFileName())
                .fileUrl(fileUrl)
                .createdAt(message.getCreatedAt())
                .build();
    }

    // Prefer Redis cache, falling back to the database when the cache is empty.
    private List<ChatMessageCacheEntry> getRecentHistory(ChatSession session) {
        return chatCacheService.getRecentMessages(session.getSessionId())
                .orElseGet(() -> loadRecentHistoryFromDatabase(session));
    }

    private List<ChatMessageCacheEntry> loadRecentHistoryFromDatabase(ChatSession session) {
        List<ChatMessage> messages = new ArrayList<>(messageRepository.findBySessionOrderByCreatedAtDesc(
                session,
                PageRequest.of(0, chatCacheService.getMaxMessages())
        ));
        java.util.Collections.reverse(messages);
        chatCacheService.replaceRecentMessages(session.getSessionId(), messages);

        return messages.stream()
                .map(this::toCacheEntry)
                .toList();
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

    // All user-facing actions require a valid Bearer token.
    private String requireUserEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Unauthorized");
        }

        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    private void requireAdmin(HttpServletRequest request) {
        String requesterEmail = requireUserEmail(request);
        Set<String> allowedAdminEmails = Arrays.stream(adminEmails == null ? new String[0] : adminEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());

        if (allowedAdminEmails.isEmpty() || !allowedAdminEmails.contains(requesterEmail)) {
            throw new RuntimeException("Forbidden");
        }
    }

    // Prevent users from reading or changing conversations they do not own.
    private ChatSession requireOwnedSession(String sessionId, String email) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getEmail().equals(email)) {
            throw new RuntimeException("Forbidden");
        }

        return session;
    }
}
