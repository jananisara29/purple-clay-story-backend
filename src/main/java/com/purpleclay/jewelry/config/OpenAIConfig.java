package com.purpleclay.jewelry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model.chat}")
    private String chatModel;

    @Value("${openai.model.image}")
    private String imageModel;

    @Value("${openai.image.size}")
    private String imageSize;

    @Value("${openai.image.quality}")
    private String imageQuality;

    @Bean
    public RestTemplate openAIRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + apiKey);
            request.getHeaders().add("Content-Type", "application/json");
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    public String getApiKey() { return apiKey; }
    public String getChatModel() { return chatModel; }
    public String getImageModel() { return imageModel; }
    public String getImageSize() { return imageSize; }
    public String getImageQuality() { return imageQuality; }
}
