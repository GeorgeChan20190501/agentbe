package com.icbc.agent.aichat.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对话数据访问层，集中管理所有会话/消息相关的SQL操作
 */
@Repository
@RequiredArgsConstructor
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_LOAD_HISTORY =
            "SELECT role, content FROM dam.ai_chat_message WHERE session_id = ? ORDER BY create_time DESC LIMIT ?";
    private static final String SQL_INSERT_SESSION =
            "INSERT INTO dam.ai_chat_session (session_id, user_id, session_name, message_count) VALUES (?, ?, ?, 0)";
    private static final String SQL_INSERT_MESSAGE =
            "INSERT INTO dam.ai_chat_message (session_id, role, content, intent_id, workflow_id) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_SESSION =
            "UPDATE dam.ai_chat_session SET last_message = ?, message_count = message_count + 2, update_time = ? WHERE session_id = ?";

    /**
     * 加载最近N轮对话历史（按时间正序返回）
     */
    public List<Map<String, Object>> loadHistory(String sessionId, int maxMessages) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL_LOAD_HISTORY, sessionId, maxMessages);
        // 反转为时间正序
        java.util.Collections.reverse(rows);
        return rows;
    }

    /**
     * 创建新会话
     * @return sessionId
     */
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(SQL_INSERT_SESSION, sessionId, userId, "新对话");
        return sessionId;
    }

    /**
     * 保存消息
     */
    public void saveMessage(String sessionId, String role, String content, String intentId, String workflowId) {
        jdbcTemplate.update(SQL_INSERT_MESSAGE, sessionId, role, content, intentId, workflowId);
    }

    /**
     * 更新会话最后一条消息和计数
     */
    public void updateSession(String sessionId, String question) {
        String lastMsg = question.length() > 50 ? question.substring(0, 50) + "..." : question;
        jdbcTemplate.update(SQL_UPDATE_SESSION, lastMsg, LocalDateTime.now(), sessionId);
    }
}
