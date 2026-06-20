package com.icbc.agent.aiintent.service.impl;

import com.icbc.agent.aiintent.entity.IntentConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IntentConfigLoader {

    private final JdbcTemplate jdbcTemplate;

    public IntentConfigLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 读取数据库意图配置，并根据id找意图示例，最终封装成IntentConfig对象
     * @return List<IntentConfig>
     */
    public List<IntentConfig> loadEnabledIntents() {
        String intentSql = "SELECT intent_id, intent_name, description, match_prompt, workflow_id " +
                "FROM ai_intent WHERE enable_flag = '是' ORDER BY sort_no";

        List<Map<String, Object>> intentRows = jdbcTemplate.queryForList(intentSql);

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
