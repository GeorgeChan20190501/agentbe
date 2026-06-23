package com.icbc.agent.aichat.service;

import com.icbc.agent.aichat.repository.ChatRepository;
import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiintent.service.IntentService;
import com.icbc.agent.aimodel.AiModelFactory;
import com.icbc.agent.aimodel.AiModelService;
import com.icbc.agent.aiworkflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final IntentService intentService;
    private final WorkflowService workflowService;
    private final AiModelFactory modelFactory;
    private final ChatRepository chatRepository;

    private static final int MAX_HISTORY_ROUNDS = 10;

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

        List<Message> historyMessages = loadHistoryMessages(sessionId);
        String contextQuestion = buildContextQuestion(historyMessages, question);

        IntentResult intentResult = intentService.recognize(contextQuestion);
        log.info("意图识别结果: intentId={}, workflowId={}, confidence={}", intentResult.getIntentId(), intentResult.getWorkflowId(), intentResult.getConfidence());

        saveMessage(sessionId, "user", question, intentResult);

        String response;
        if (!intentResult.isNormalChat()) {
            intentResult.setSessionId(sessionId);
            intentResult.setQuestion(question);
            response = workflowService.execute(intentResult);
            if (response == null) response = "工作流执行无结果";
        } else {
            AiModelService modelService = modelFactory.getDefaultModel();
            response = modelService.chat(contextQuestion);
        }

        saveMessage(sessionId, "assistant", response, intentResult);
        chatRepository.updateSession(sessionId, question);

        return new ChatResult(sessionId, response);
    }

    public Flux<Object> streamChat(String question, String sessionId) {
        sessionId = ensureSession(sessionId);
        final String finalSessionId = sessionId;

        List<Message> historyMessages = loadHistoryMessages(sessionId);
        String contextQuestion = buildContextQuestion(historyMessages, question);

        IntentResult intentResult = intentService.recognize(contextQuestion);
        log.info("流式意图识别: intentId={}, workflowId={}", intentResult.getIntentId(), intentResult.getWorkflowId());

        saveMessage(sessionId, "user", question, intentResult);

        Flux<Object> contentFlux;
        if (!intentResult.isNormalChat()) {
            intentResult.setSessionId(sessionId);
            intentResult.setQuestion(question);
            contentFlux = workflowService.executeStream(intentResult).doOnNext(obj -> {
                log.info("工作流服务返回了: {}", obj);
            });
        } else {
            contentFlux = modelFactory.getDefaultModel().streamChat(contextQuestion);
        }

        // 收集流式响应内容，流结束后保存到数据库
        StringBuilder responseCollector = new StringBuilder();
        contentFlux = contentFlux.doOnNext(obj -> {
            if (obj instanceof String) {
                responseCollector.append((String) obj);
            }
        }).doOnComplete(() -> {
            String response = responseCollector.toString();
            if (!response.isEmpty()) {
                try {
                    saveMessage(finalSessionId, "assistant", response, intentResult);
                    chatRepository.updateSession(finalSessionId, question);
                } catch (Exception e) {
                    log.error("流式响应保存失败", e);
                }
            }
        });

        Flux<Object> sessionEvent = Flux.just(new SessionEvent(finalSessionId));
        return sessionEvent.concatWith(contentFlux);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SessionEvent {
        private String sessionId;
    }

    // ==================== 内部方法 ====================

    private String ensureSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return chatRepository.createSession("default_user");
        }
        return sessionId;
    }

    /**
     * 加载历史对话并转换为Spring AI Message结构化列表
     */
    private List<Message> loadHistoryMessages(String sessionId) {
        List<Map<String, Object>> rows = chatRepository.loadHistory(sessionId, MAX_HISTORY_ROUNDS * 2);
        List<Message> messages = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String role = (String) row.get("role");
            String content = (String) row.get("content");
            if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            }
        }
        return messages;
    }

    /**
     * 将结构化历史消息 + 当前问题拼接为上下文感知的提问文本
     */
    private String buildContextQuestion(List<Message> history, String question) {
        if (history.isEmpty()) {
            return question;
        }
        StringBuilder sb = new StringBuilder("以下是历史对话:\n");
        for (Message msg : history) {
            String role = msg instanceof UserMessage ? "user" : "assistant";
            sb.append(role).append(": ").append(msg.getText()).append("\n");
        }
        sb.append("用户当前问题: ").append(question);
        return sb.toString();
    }

    private void saveMessage(String sessionId, String role, String content, IntentResult intentResult) {
        String intentId = intentResult != null ? intentResult.getIntentId() : "";
        String workflowId = intentResult != null ? intentResult.getWorkflowId() : "";
        chatRepository.saveMessage(sessionId, role, content, intentId, workflowId);
    }
}
