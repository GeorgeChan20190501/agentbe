package com.icbc.agent.aiskill.service.imp;

import com.alibaba.fastjson2.JSON;
import com.icbc.agent.aicommon.chat.bean.WorkflowContext;
import com.icbc.agent.aimodel.AiModelFactory;
import com.icbc.agent.aimodel.AiModelService;
import com.icbc.agent.aiskill.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 技能服务实现
 * 从数据库加载技能配置，填充提示词模板，调用大模型生成结果
 */
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final JdbcTemplate jdbcTemplate;
    private final AiModelFactory modelFactory;

    @Override
    public String execute(String skillId, WorkflowContext context) {
        Map<String, Object> skill = loadSkill(skillId);
        if (skill == null) {
            return "技能不存在: " + skillId;
        }

        String systemPrompt = (String) skill.get("system_prompt");
        String userPromptTemplate = (String) skill.get("user_prompt");
        String modelName = (String) skill.get("model_name");

        String userPrompt = fillPromptTemplate(userPromptTemplate, context);

        AiModelService model = getModel(modelName);
        return model.chat(systemPrompt + "\n\n" + userPrompt);
    }

    @Override
    public Flux<Object> executeStream(String skillId, WorkflowContext context) {
        Map<String, Object> skill = loadSkill(skillId);
        if (skill == null) {
            return Flux.just("技能不存在: " + skillId);
        }

        String systemPrompt = (String) skill.get("system_prompt");
        String userPromptTemplate = (String) skill.get("user_prompt");
        String modelName = (String) skill.get("model_name");

        String userPrompt = fillPromptTemplate(userPromptTemplate, context);

        AiModelService model = getModel(modelName);
        return model.streamChat(systemPrompt + "\n\n" + userPrompt);
    }

    private Map<String, Object> loadSkill(String skillId) {
        String sql = "SELECT skill_id, skill_name, system_prompt, user_prompt, out_schema, model_name, temperature " +
                "FROM ai_skill WHERE skill_id = ? AND enable_flag = '是'";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, skillId);
        return results.isEmpty() ? null : results.get(0);
    }

    private String fillPromptTemplate(String template, WorkflowContext context) {
        String result = template;
        for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? JSON.toJSONString(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private AiModelService getModel(String modelName) {
        if (modelName != null && !modelName.isEmpty()) {
            try {
                return modelFactory.getModel(modelName);
            } catch (Exception e) {
                // fallback to default
            }
        }
        return modelFactory.getDefaultModel();
    }
}
