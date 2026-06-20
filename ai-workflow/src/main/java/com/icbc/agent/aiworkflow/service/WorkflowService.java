package com.icbc.agent.aiworkflow.service;

import com.icbc.agent.aiintent.entity.IntentResult;
import reactor.core.publisher.Flux;

public interface WorkflowService {

    String execute(IntentResult question);

    Flux<Object> executeStream(IntentResult intentResult);
}
