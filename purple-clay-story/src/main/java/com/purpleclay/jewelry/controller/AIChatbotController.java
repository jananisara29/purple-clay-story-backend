package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.ai.AIChatbotService;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI - Chatbot", description = "GPT-4 powered shopping assistant with conversation history")
public class AIChatbotController {

    private final AIChatbotService chatbotService;
    private final UserRepository userRepository;

    @PostMapping("/session")
    @Operation(summary = "Start a new chat session — returns sessionId")
    public ResponseEntity<Map<String, String>> startSession() {
        String sessionId = chatbotService.startSession();
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "message", "Hi! I'm Priya, your Purple Clay Story assistant. How can I help you today? 🌸"
        ));
    }

    @PostMapping("/session/{sessionId}/message")
    @Operation(summary = "Send a message in a session — returns AI reply + updated history",
               description = "Pass sessionId from /session. Optionally pass conversation history in body for stateless clients.")
    public ResponseEntity<AIDTOs.AIChatResponse> chat(
        @PathVariable String sessionId,
        @RequestBody AIDTOs.AIChatRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(chatbotService.chat(sessionId, userId, request));
    }

    @GetMapping("/session/{sessionId}/history")
    @Operation(summary = "Get full conversation history for a session")
    public ResponseEntity<List<AIDTOs.AIChatMessage>> getHistory(
        @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(chatbotService.getHistory(sessionId));
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Clear/delete a chat session")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatbotService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
            .map(u -> u.getId())
            .orElse(null);
    }
}
