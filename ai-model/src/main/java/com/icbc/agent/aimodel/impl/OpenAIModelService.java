package com.icbc.agent.aimodel.impl;

import com.icbc.agent.aimodel.AiModelService;
import reactor.core.publisher.Flux;

/**
 * OpenAI模型实现（预留）
 * 待集成Spring AI OpenAI starter后启用
 */
// @Service("openaiModel")  // 暂时注释，集成后启用
public class OpenAIModelService implements AiModelService {

    // TODO: 注入OpenAI ChatClient
    // private final ChatClient chatClient;

    @Override
    public String chat(String message) {
        // TODO: 实现OpenAI调用
        throw new UnsupportedOperationException("OpenAI模型尚未集成");
    }

    @Override
    public Flux<Object> streamChat(String message) {
        return null;
    }

    @Override
    public String getModelName() {
        return "openai";
    }
}
