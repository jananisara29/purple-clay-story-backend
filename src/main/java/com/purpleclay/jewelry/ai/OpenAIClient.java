package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

    private final RestTemplate openAIRestTemplate;
    private final OpenAIConfig openAIConfig;

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String IMAGE_URL = "https://api.openai.com/v1/images/generations";

    /**
     * Call GPT-4 chat completion
     */
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getChatModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ),
            "max_tokens", 1000,
            "temperature", 0.7
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
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("OpenAI chat API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable: " + e.getMessage());
        }
    }

    /**
     * Call DALL-E 3 image generation
     */
    public String generateImage(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getImageModel(),
            "prompt", prompt,
            "n", 1,
            "size", openAIConfig.getImageSize(),
            "quality", openAIConfig.getImageQuality()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = openAIRestTemplate.postForEntity(
                IMAGE_URL,
                new HttpEntity<>(requestBody, headers),
                Map.class
            );

            Map body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from OpenAI");

            List<Map> data = (List<Map>) body.get("data");
            return (String) data.get(0).get("url");

        } catch (Exception e) {
            log.error("OpenAI image API call failed: {}", e.getMessage());
            throw new RuntimeException("AI image service unavailable: " + e.getMessage());
        }
    }
}
