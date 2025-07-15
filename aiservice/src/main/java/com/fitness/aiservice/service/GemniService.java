package com.fitness.aiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GemniService {

    private final WebClient webClient;

    @Value("${gemni.api.url}")
    private String gemniApiUrl;

    @Value("${gemni.api.key}")
    private String gemniApiKey;

    public GemniService(WebClient.Builder webClientBuilder){
        this.webClient=webClientBuilder.build();
    }

    public String getAnswer(String question){
           Map<String,Object> requestBody=Map.of(
                   "contents",new Object[]{
                           Map.of("parts",new Object[]{
                                   Map.of("text",question)
                           })
                   }
           );

           String response=webClient.post()
                   .uri(gemniApiUrl+gemniApiKey)
                   .header("Content-Type","application/json")
                   .bodyValue(requestBody)
                   .retrieve()
                   .bodyToMono(String.class)
                   .block();

           return response;
    }
}
