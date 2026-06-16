package com.icbc.agent.aichat.service;

import com.icbc.agent.aimodel.AiModelFactory;
import com.icbc.agent.aimodel.AiModelService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 聊天服务
 * 通过模型工厂调用底层AI模型
 */
@Service
public class ChatService {

    private final AiModelFactory modelFactory;

    public ChatService(AiModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    /**
     * 调用AI模型进行对话（使用默认模型）
     * @param message 用户输入的消息
     * @return AI回复的内容
     */
    public String chat(String message) {
        AiModelService model = modelFactory.getDefaultModel();
        return model.chat(message);
    }
    
    /**
     * 调用AI模型进行流式对话（使用默认模型）
     * @param message 用户输入的消息
     * @return 流式AI回复内容
     */
    public Flux<Object> streamChat(String message) {
        AiModelService model = modelFactory.getDefaultModel();
        return model.streamChat(message);
    }
    
    /**
     * 调用指定AI模型进行对话
     * @param message 用户输入的消息
     * @param provider 模型提供者：qwen, deepseek, openai等
     * @return AI回复的内容
     */
    public String chat(String message, String provider) {
        AiModelService model = modelFactory.getModel(provider);
        return model.chat(message);
    }
    
    /**
     * 获取当前使用的模型名称
     */
    public String getCurrentModel() {
        return modelFactory.getDefaultProvider();
    }
}
