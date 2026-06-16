package com.icbc.agent.aimodel.impl;

import com.icbc.agent.aimodel.AiModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
/**
 * 通义千问模型实现
 */
@Service("qwenModel")
public class QwenModelService implements AiModelService {

    private final ChatClient chatClient;

    public QwenModelService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @Override
    public Flux<Object> streamChat(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .chatResponse()
                .flatMap(response -> {
                    if (response != null && response.getResult() != null) {
                        String content = response.getResult().getOutput().getText();
                        if (content != null && !content.isEmpty()) {
                            return Flux.just(content);
                        }
                    }
                    return Flux.empty();
                });
    }

    @Override
    public String getModelName() {
        return "qwen";
    }
}
