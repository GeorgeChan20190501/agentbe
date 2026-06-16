package com.icbc.agent.aimodel;

import reactor.core.publisher.Flux;

/**
 * AI模型服务接口
 * 定义统一的对话接口，支持不同模型的切换
 */
public interface AiModelService {
    
    /**
     * 发送消息并获取回复
     * @param message 用户消息
     * @return AI回复内容
     */
    String chat(String message);
    
    /**
     * 发送消息并获取流式回复
     *
     * @param message 用户消息
     * @return 流式AI回复内容
     */
    Flux<Object> streamChat(String message);
    
    /**
     * 获取模型名称
     * @return 模型标识
     */
    String getModelName();
}
