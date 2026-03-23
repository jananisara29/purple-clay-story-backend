package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.ChatMessage;
import com.purpleclay.jewelry.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatbotService {

    private final OpenAIClient openAIClient;
    private final ChatContextBuilder chatContextBuilder;
    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate openAIRestTemplate;

    // Max messages to keep in context window (older ones are trimmed)
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Start a new chat session. Returns a new sessionId.
     */
    public String startSession() {
        return UUID.randomUUID().toString();
    }

    /**
     * Send a message in an existing session and get a reply.
     * Persists both user message and assistant reply to DB.
     */
    @Transactional
    public AIDTOs.AIChatResponse chat(String sessionId, Long userId, AIDTOs.AIChatRequest request) {
        // 1. Load existing session history from DB
        List<ChatMessage> dbHistory = chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);

        // 2. Trim to last N messages for context window
        List<ChatMessage> trimmedHistory = trimHistory(dbHistory);

        // 3. Build OpenAI messages list
        List<Map<String, String>> openAIMessages = new ArrayList<>();
        openAIMessages.add(Map.of("role", "system", "content", chatContextBuilder.buildSystemPrompt()));

        // Add conversation history
        trimmedHistory.forEach(msg ->
            openAIMessages.add(Map.of("role", msg.getRole(), "content", msg.getContent()))
        );

        // Add new user message
        openAIMessages.add(Map.of("role", "user", "content", request.message()));

        // 4. Call GPT-4
        String reply = callGPT4WithHistory(openAIMessages);
        log.debug("Chatbot reply for session {}: {}", sessionId, reply.substring(0, Math.min(80, reply.length())));

        // 5. Persist user message
        chatMessageRepository.save(ChatMessage.builder()
            .sessionId(sessionId)
            .userId(userId)
            .role("user")
            .content(request.message())
            .build());

        // 6. Persist assistant reply
        chatMessageRepository.save(ChatMessage.builder()
            .sessionId(sessionId)
            .userId(userId)
            .role("assistant")
            .content(reply)
            .build());

        // 7. Return updated history as DTOs
        List<AIDTOs.AIChatMessage> updatedHistory = buildDTOHistory(trimmedHistory, request.message(), reply);

        return new AIDTOs.AIChatResponse(reply, updatedHistory);
    }

    /**
     * Get full conversation history for a session.
     */
    public List<AIDTOs.AIChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId)
            .stream()
            .map(msg -> new AIDTOs.AIChatMessage(msg.getRole(), msg.getContent()))
            .collect(Collectors.toList());
    }

    /**
     * Delete a chat session.
     */
    @Transactional
    public void clearSession(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        log.info("Chat session cleared: {}", sessionId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String callGPT4WithHistory(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o",
            "messages", messages,
            "max_tokens", 400,
            "temperature", 0.8  // slightly more creative for chat
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = openAIRestTemplate.postForEntity(
                CHAT_URL,
                new HttpEntity<>(requestBody, headers),
                Map.class
            );

            Map body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from OpenAI");

            List<Map> choices = (List<Map>) body.get("choices");
            Map message = (Map) choices.get(0).get("message");
            return ((String) message.get("content")).trim();

        } catch (Exception e) {
            log.error("GPT-4 chat call failed: {}", e.getMessage());
            return "I'm having trouble connecting right now. Please try again in a moment! 🌸";
        }
    }

    private List<ChatMessage> trimHistory(List<ChatMessage> history) {
        if (history.size() <= MAX_HISTORY_MESSAGES) return history;
        return history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size());
    }

    private List<AIDTOs.AIChatMessage> buildDTOHistory(
        List<ChatMessage> existing, String newUserMsg, String assistantReply
    ) {
        List<AIDTOs.AIChatMessage> result = existing.stream()
            .map(m -> new AIDTOs.AIChatMessage(m.getRole(), m.getContent()))
            .collect(Collectors.toList());
        result.add(new AIDTOs.AIChatMessage("user", newUserMsg));
        result.add(new AIDTOs.AIChatMessage("assistant", assistantReply));
        return result;
    }
}
