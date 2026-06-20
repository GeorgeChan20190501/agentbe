package com.icbc.agent.aimodel.impl;

import com.icbc.agent.aimodel.AiModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * 通义千问模型实现
 */
@Service("qwenModel")
public class QwenModelService implements AiModelService {

    private final ChatClient chatClient;

    public QwenModelService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE");

    private String buildSystemPrompt() {
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
        return "你是AI助手。当前时间：" + currentDate;
    }

    @Override
    public String chat(String message) {
        return chatClient.prompt()
                .system(buildSystemPrompt())
                .user(message)
                .call()
                .content();
    }

    @Override
    public Flux<Object> streamChat(String message) {
        return chatClient.prompt()
                .system(buildSystemPrompt())
                .user(message)
                .stream()
                .chatResponse()
                .flatMap(response -> {
                    response.getResult();
                    String content = response.getResult().getOutput().getText();
                    if (content != null && !content.isEmpty()) {
                        return Flux.just(content);
                    }
                    return Flux.empty();
                });
    }

    @Override
    public String getModelName() {
        return "qwen";
    }
}
