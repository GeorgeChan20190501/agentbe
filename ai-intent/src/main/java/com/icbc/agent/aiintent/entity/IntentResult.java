package com.icbc.agent.aiintent.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IntentResult {
    private String sessionId;
    private String question;
    private String intentId;
    private String intentName;
    private String workflowId;
    private double confidence;

    public IntentResult(String intentId, String intentName, String workflowId, double confidence) {
        this.intentId = intentId;
        this.intentName = intentName;
        this.workflowId = workflowId;
        this.confidence = confidence;
    }

    public boolean isNormalChat() {
        return workflowId == null || workflowId.isEmpty();
    }
}
