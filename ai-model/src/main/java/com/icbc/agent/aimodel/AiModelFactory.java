package com.icbc.agent.aimodel;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI模型工厂
 * 根据配置动态选择模型实现
 */
@Component
public class AiModelFactory {

    private final Map<String, AiModelService> modelServices;

    /**
     * -- GETTER --
     *  获取当前默认模型名称
     */
    @Getter
    @Value("${ai.model.provider:qwen}")
    private String defaultProvider;

    public AiModelFactory(Map<String, AiModelService> modelServices) {
        this.modelServices = modelServices;
    }

    /**
     * 获取默认模型服务
     */
    public AiModelService getDefaultModel() {
        return getModel(defaultProvider);
    }

    /**
     * 根据提供者名称获取模型服务
     * @param provider 模型提供者：qwen, deepseek, openai等
     */
    public AiModelService getModel(String provider) {
        String beanName = provider + "Model";
        AiModelService service = modelServices.get(beanName);
        
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型提供者: " + provider 
                + ", 当前支持的模型: " + modelServices.keySet());
        }
        
        return service;
    }

}
