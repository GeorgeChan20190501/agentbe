package com.icbc.agent.aiintent.service;

import com.icbc.agent.aiintent.entity.IntentResult;

public interface IntentService {
    IntentResult recognize(String question);
}
