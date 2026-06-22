package com.icbc.agent.aicommon.chat.bean;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowContext {

    private String sessionId;

    private String question;

    private List<AiChatMessage> history;

    private String workflowId;

    private String intentId;

    /**
     * 工具执行结果
     */
    private Map<String,Object> variables = new HashMap<>();


    public void put(String key,Object value){
        variables.put(key,value);
    }

    public Object get(String key){
        return variables.get(key);
    }

    public <T> T get(String key,Class<T> clazz){
        return clazz.cast(variables.get(key));
    }

    public Object getOrDefault(String key, Object defaultValue){
        return variables.getOrDefault(key, defaultValue);
    }

}
