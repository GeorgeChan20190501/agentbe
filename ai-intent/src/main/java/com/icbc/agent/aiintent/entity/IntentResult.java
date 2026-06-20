package com.icbc.agent.aiintent.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentResult {

    private String intentId;
    private String intentName;
    private String workflowId;
    private double confidence;

    public boolean isNormalChat() {
        return workflowId == null || workflowId.isEmpty();
    }
}
