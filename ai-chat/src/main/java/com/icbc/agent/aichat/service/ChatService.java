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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final IntentService intentService;
    private final WorkflowService workflowService;
    private final AiModelFactory modelFactory;
    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_HISTORY_ROUNDS = 10;
    private static final String SQL_LOAD_HISTORY =
            "SELECT role, content FROM ai_chat_message WHERE session_id = ? ORDER BY create_time DESC LIMIT ?";
    private static final String SQL_INSERT_SESSION =
            "INSERT INTO ai_chat_session (session_id, user_id, session_name, message_count) VALUES (?, ?, ?, 0)";
    private static final String SQL_INSERT_MESSAGE =
            "INSERT INTO ai_chat_message (session_id, role, content, intent_id, workflow_id) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_SESSION =
            "UPDATE ai_chat_session SET last_message = ?, message_count = message_count + 2, update_time = ? WHERE session_id = ?";


    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChatResult {
        private String sessionId;
        private String content;
    }

    public ChatResult chat(String question) {
        return chat(question, null);
    }

    public ChatResult chat(String question, String sessionId) {
        sessionId = ensureSession(sessionId);

        String history = loadHistory(sessionId);
        String contextQuestion = buildContextQuestion(history, question);

        IntentResult intentResult = intentService.recognize(question);
        log.info("意图识别结果: intentId={}, workflowId={}, confidence={}", intentResult.getIntentId(), intentResult.getWorkflowId(), intentResult.getConfidence());

        saveMessage(sessionId, "user", question, intentResult);

        String response;
        if (!intentResult.isNormalChat()) {
            response = workflowService.execute(intentResult);
            if (response == null) response = "工作流执行无结果";
        } else {
            AiModelService modelService = modelFactory.getDefaultModel();
            response = modelService.chat(contextQuestion);
        }

        saveMessage(sessionId, "assistant", response, intentResult);
        updateSession(sessionId, question);

        return new ChatResult(sessionId, response);
    }

    public Flux<Object> streamChat(String question, String sessionId) {
        //1.判断是否传入了sessionId，如果没有则创建一个
        sessionId = ensureSession(sessionId);
        final String finalSessionId = sessionId;
        String history = loadHistory(sessionId);
        String contextQuestion = buildContextQuestion(history, question);

        // 2.从数据库检索意图配置，结合用户输入+系统提示词，丢给轻量快速模型去进行意图识别，返回意图结果
        IntentResult intentResult = intentService.recognize(contextQuestion);
        log.info("流式意图识别: intentId={}, workflowId={}", intentResult.getIntentId(), intentResult.getWorkflowId());

        // 3.保存用户消息到数据库
        saveMessage(sessionId, "user", question, intentResult);
        Flux<Object> contentFlux;
        // 4.判断意图结果，如果识别到工作流，则调用工作流服务
        if (!intentResult.isNormalChat()) {
            // 调用工作流服务
            contentFlux = workflowService.executeStream(intentResult).doOnNext(obj -> {
                log.info("工作流服务返回了: {}", obj);
            });

        }else {
            contentFlux = modelFactory.getDefaultModel().streamChat(contextQuestion);
        }


        // 如果是正常对话，则使用快速模型进行生成
        Flux<Object> sessionEvent = Flux.just(new SessionEvent(finalSessionId));
        return sessionEvent.concatWith(contentFlux);
    }


    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SessionEvent {
        private String sessionId;
    }


    private String ensureSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession("default_user");
        }
        return sessionId;
    }

    private String loadHistory(String sessionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL_LOAD_HISTORY, sessionId, MAX_HISTORY_ROUNDS * 2);
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = rows.size() - 1; i >= 0; i--) {
            Map<String, Object> row = rows.get(i);
            sb.append(row.get("role")).append(": ").append(row.get("content")).append("\n");
        }
        return sb.toString();
    }

    private String buildContextQuestion(String history, String question) {
        if (history.isEmpty()) {
            return question;
        }
        return "以下是历史对话:\n" + history + "用户当前问题: " + question;
    }
    private String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(SQL_INSERT_SESSION, sessionId, userId, "新对话");
        return sessionId;
    }
    private void saveMessage(String sessionId, String role, String content, IntentResult intentResult) {
        String intentId = intentResult != null ? intentResult.getIntentId() : "";
        String workflowId = intentResult != null ? intentResult.getWorkflowId() : "";
        jdbcTemplate.update(SQL_INSERT_MESSAGE, sessionId, role, content, intentId, workflowId);
    }

    private void updateSession(String sessionId, String question) {
        String lastMsg = question.length() > 50 ? question.substring(0, 50) + "..." : question;
        jdbcTemplate.update(SQL_UPDATE_SESSION, lastMsg, LocalDateTime.now(), sessionId);
    }
}
