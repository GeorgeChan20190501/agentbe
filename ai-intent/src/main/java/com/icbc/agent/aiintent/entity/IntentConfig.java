package com.icbc.agent.aiintent.entity;

import lombok.Data;
import java.util.List;

@Data
public class IntentConfig {
    private String intentId;
    private String intentName;
    private String description;
    private String matchPrompt;
    private String workflowId;
    private List<String> exampleQuestions;
}
