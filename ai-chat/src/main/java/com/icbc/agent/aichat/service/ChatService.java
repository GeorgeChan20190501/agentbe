package com.icbc.agent.aichat.service;

import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiintent.service.IntentService;
import com.icbc.agent.aimodel.AiModelFactory;
import com.icbc.agent.aimodel.AiModelService;
import com.icbc.agent.aiworkflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final IntentService intentService;
    private final WorkflowService workflowService;
    private final AiModelFactory modelFactory;
    private final JdbcTemplate jdbcTemplate;

    public String chat(String question) {
        return chat(question, null);
    }

    public String chat(String question, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession("default_user");
        }

        IntentResult intentResult = intentService.recognize(question);
        log.info("意图识别结果: intentId={}, workflowId={}, confidence={}",
                intentResult.getIntentId(), intentResult.getWorkflowId(), intentResult.getConfidence());

        saveMessage(sessionId, "user", question, intentResult);

        String response;
        if (!intentResult.isNormalChat()) {
            response = workflowService.execute(intentResult);
            if (response == null) response = "工作流执行无结果";
        } else {
            AiModelService modelService = modelFactory.getDefaultModel();
            response = modelService.chat(question);
        }

        saveMessage(sessionId, "assistant", response, intentResult);
        updateSession(sessionId, question, response);

        return response;
    }

    public Flux<Object> streamChat(String question) {
        return streamChat(question, null);
    }

    public Flux<Object> streamChat(String question, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession("default_user");
        }

        // 从数据库检索意图配置，结合用户输入+系统提示词，丢给轻量快速模型去进行意图识别，返回意图结果
        IntentResult intentResult = intentService.recognize(question);
        log.info("流式意图识别: intentId={}, workflowId={}", intentResult.getIntentId(), intentResult.getWorkflowId());

        // 保存用户消息到数据库
        saveMessage(sessionId, "user", question, intentResult);


        final String finalSessionId = sessionId;
        if (!intentResult.isNormalChat()) {
            // 如果是工作流，则调用工作流服务
            return workflowService.executeStream(intentResult)
                    .doOnNext(obj -> {
                    });
        }

        // 如果是正常对话，则使用快速模型进行生成
        AiModelService modelService = modelFactory.getDefaultModel();
        return modelService.streamChat(question);
    }

    private String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(
                "INSERT INTO ai_chat_session (session_id, user_id, session_name, message_count) VALUES (?, ?, ?, 0)",
                sessionId, userId, "新对话"
        );
        return sessionId;
    }

    private void saveMessage(String sessionId, String role, String content, IntentResult intentResult) {
        jdbcTemplate.update(
                "INSERT INTO ai_chat_message (session_id, role, content, intent_id, workflow_id) VALUES (?, ?, ?, ?, ?)",
                sessionId, role, content,
                intentResult != null ? intentResult.getIntentId() : "",
                intentResult != null ? intentResult.getWorkflowId() : ""
        );
    }

    private void updateSession(String sessionId, String question, String response) {
        String lastMsg = question.length() > 50 ? question.substring(0, 50) + "..." : question;
        jdbcTemplate.update(
                "UPDATE ai_chat_session SET last_message = ?, message_count = message_count + 2, update_time = ? WHERE session_id = ?",
                lastMsg, LocalDateTime.now(), sessionId
        );
    }
}
