package com.purpleclay.jewelry;

import com.purpleclay.jewelry.ai.AIChatbotService;
import com.purpleclay.jewelry.ai.ChatContextBuilder;
import com.purpleclay.jewelry.ai.OpenAIClient;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.ChatMessage;
import com.purpleclay.jewelry.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIChatbotServiceTest {

    @Mock private OpenAIClient openAIClient;
    @Mock private ChatContextBuilder chatContextBuilder;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private RestTemplate openAIRestTemplate;

    @InjectMocks private AIChatbotService chatbotService;

    private static final String SESSION_ID = "test-session-123";

    @BeforeEach
    void setUp() {
        when(chatContextBuilder.buildSystemPrompt()).thenReturn("You are Priya...");
    }

    @Test
    void startSession_returnsNonNullUUID() {
        String sessionId = chatbotService.startSession();
        assertNotNull(sessionId);
        assertFalse(sessionId.isBlank());
        assertEquals(36, sessionId.length()); // UUID length
    }

    @Test
    void chat_emptyHistory_callsGPTAndPersistsBothMessages() {
        when(chatMessageRepository.findBySessionIdOrderByCreatedAt(SESSION_ID))
            .thenReturn(List.of());

        // Mock OpenAI REST response
        Map<String, Object> mockResponse = buildMockOpenAIResponse("I'd love to help you find the perfect piece!");
        when(openAIRestTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok(mockResponse));

        AIDTOs.AIChatRequest request = new AIDTOs.AIChatRequest("Show me earrings under ₹500", null);
        AIDTOs.AIChatResponse response = chatbotService.chat(SESSION_ID, 1L, request);

        assertNotNull(response);
        assertEquals("I'd love to help you find the perfect piece!", response.reply());
        assertEquals(2, response.updatedHistory().size()); // user + assistant
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void chat_withExistingHistory_includesContextInRequest() {
        List<ChatMessage> existingHistory = List.of(
            ChatMessage.builder().role("user").content("Hi").sessionId(SESSION_ID).build(),
            ChatMessage.builder().role("assistant").content("Hello! How can I help?").sessionId(SESSION_ID).build()
        );
        when(chatMessageRepository.findBySessionIdOrderByCreatedAt(SESSION_ID))
            .thenReturn(existingHistory);

        Map<String, Object> mockResponse = buildMockOpenAIResponse("Here are some earrings for you!");
        when(openAIRestTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok(mockResponse));

        AIDTOs.AIChatRequest request = new AIDTOs.AIChatRequest("Show me earrings", null);
        AIDTOs.AIChatResponse response = chatbotService.chat(SESSION_ID, 1L, request);

        // History = 2 existing + 1 user + 1 assistant = 4
        assertEquals(4, response.updatedHistory().size());
    }

    @Test
    void getHistory_returnsMessagesAsDTOs() {
        List<ChatMessage> messages = List.of(
            ChatMessage.builder().role("user").content("Hello").sessionId(SESSION_ID).build(),
            ChatMessage.builder().role("assistant").content("Hi there!").sessionId(SESSION_ID).build()
        );
        when(chatMessageRepository.findBySessionIdOrderByCreatedAt(SESSION_ID)).thenReturn(messages);

        List<AIDTOs.AIChatMessage> history = chatbotService.getHistory(SESSION_ID);

        assertEquals(2, history.size());
        assertEquals("user", history.get(0).role());
        assertEquals("assistant", history.get(1).role());
    }

    @Test
    void clearSession_deletesMessages() {
        chatbotService.clearSession(SESSION_ID);
        verify(chatMessageRepository).deleteBySessionId(SESSION_ID);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildMockOpenAIResponse(String content) {
        Map<String, String> message = Map.of("role", "assistant", "content", content);
        Map<String, Object> choice = Map.of("message", message);
        return Map.of("choices", List.of(choice));
    }
}
