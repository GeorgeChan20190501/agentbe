package com.icbc.agent.aiintent.service.impl;

import com.icbc.agent.aiintent.entity.IntentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IntentConfigLoader {

    private final JdbcTemplate jdbcTemplate;

    /** 缓存过期时间（毫秒） */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** 缓存的意图配置 */
    private volatile List<IntentConfig> cachedIntents;
    /** 缓存加载时间戳 */
    private volatile long cacheTimestamp;

    /**
     * 获取已启用的意图配置（带本地缓存，5分钟过期）
     * @return List<IntentConfig>
     */
    public List<IntentConfig> loadEnabledIntents() {
        long now = System.currentTimeMillis();
        if (cachedIntents != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedIntents;
        }
        synchronized (this) {
            // double-check
            if (cachedIntents != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
                return cachedIntents;
            }
            cachedIntents = doLoadEnabledIntents();
            cacheTimestamp = System.currentTimeMillis();
            return cachedIntents;
        }
    }

    /**
     * 强制刷新缓存（供管理接口调用）
     */
    public void refreshCache() {
        synchronized (this) {
            cachedIntents = null;
            cacheTimestamp = 0;
        }
    }

    private List<IntentConfig> doLoadEnabledIntents() {

        List<Map<String, Object>> intentRows = jdbcTemplate.queryForList("SELECT intent_id, intent_name, description, match_prompt, workflow_id " +
                "FROM ai_intent WHERE enable_flag = '是' ORDER BY sort_no");

        if (intentRows.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> intentIds = intentRows.stream()
                .map(r -> (String) r.get("intent_id"))
                .toList();

        String exampleSql = "SELECT intent_id, question FROM ai_intent_example WHERE intent_id IN (" +
                intentIds.stream().map(id -> "'" + id + "'").collect(Collectors.joining(",")) + ")";
        List<Map<String, Object>> exampleRows = jdbcTemplate.queryForList(exampleSql);

        Map<String, List<String>> examplesMap = exampleRows.stream()
                .collect(Collectors.groupingBy(
                        r -> (String) r.get("intent_id"),
                        Collectors.mapping(r -> (String) r.get("question"), Collectors.toList())
                ));

        return intentRows.stream().map(row -> {
            IntentConfig config = new IntentConfig();
            config.setIntentId((String) row.get("intent_id"));
            config.setIntentName((String) row.get("intent_name"));
            config.setDescription((String) row.get("description"));
            config.setMatchPrompt((String) row.get("match_prompt"));
            config.setWorkflowId((String) row.get("workflow_id"));
            config.setExampleQuestions(examplesMap.getOrDefault(config.getIntentId(), new ArrayList<>()));
            return config;
        }).collect(Collectors.toList());
    }
}
