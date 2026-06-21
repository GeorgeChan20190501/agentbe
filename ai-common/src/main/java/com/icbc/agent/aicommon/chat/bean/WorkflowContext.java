package com.icbc.agent.aicommon.chat.bean;

import lombok.Data;

import java.util.Map;

@Data
public class WorkflowContext {

    private String sessionId;
    private String question;
    private Map<String, Object> variables;

}
