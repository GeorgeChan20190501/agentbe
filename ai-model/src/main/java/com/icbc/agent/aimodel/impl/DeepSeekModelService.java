package com.icbc.agent.aimodel.impl;

import com.icbc.agent.aimodel.AiModelService;
import reactor.core.publisher.Flux;

/**
 * DeepSeek模型实现（预留）
 * 待集成Spring AI DeepSeek starter后启用
 */
// @Service("deepseekModel")  // 暂时注释，集成后启用
public class DeepSeekModelService implements AiModelService {

    // TODO: 注入DeepSeek ChatClient
    // private final ChatClient chatClient;

    @Override
    public String chat(String message) {
        // TODO: 实现DeepSeek调用
        throw new UnsupportedOperationException("DeepSeek模型尚未集成");
    }

    @Override
    public Flux<Object> streamChat(String message) {
        return null;
    }

    @Override
    public String getModelName() {
        return "deepseek";
    }
}
