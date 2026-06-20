package com.icbc.agent.aiintent.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.icbc.agent.aiintent.entity.IntentConfig;
import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiintent.service.IntentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntentServiceImpl implements IntentService {

    private final ChatClient chatClient;
    private final IntentConfigLoader intentConfigLoader;

    public IntentServiceImpl(ChatClient chatClient, IntentConfigLoader intentConfigLoader) {
        this.chatClient = chatClient;
        this.intentConfigLoader = intentConfigLoader;
    }

    @Override
    public IntentResult recognize(String question) {
        List<IntentConfig> intents = intentConfigLoader.loadEnabledIntents();

        if (intents.isEmpty()) {
            return new IntentResult(null, "普通对话", null, 1.0);
        }

        // 构建意图列表的 JSON 字符串，用于填充系统提示词模板中
        String intentListJson = buildIntentListPrompt(intents);

        String systemPrompt = """
                你是一个意图识别引擎。根据用户问题，从候选意图中匹配最合适的意图。
                
                候选意图列表（JSON格式）：
                %s
                
                匹配规则：
                1. 综合考虑意图名称、描述、匹配提示词和示例问题
                2. 如果没有任何意图匹配，返回 intentId 为空字符串
                3. 返回JSON格式：{"intentId":"xxx","confidence":0.95}
                
                只返回JSON，不要返回其他内容。
                """.formatted(intentListJson);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        // 解析大模型的响应，获取意图结果
        return parseIntentResult(response, intents);
    }

    /**
     * 将意图配置列表构建为 JSON 格式的提示词片段，供大模型进行意图匹配。
     * <p>每个意图会被序列化为包含 intentId、intentName、description、matchPrompt 和 examples 的 JSON 对象，
     * 最终拼接为 JSON 数组字符串嵌入到系统提示词中。</p>
     *
     * @param intents 已启用的意图配置列表，不能为空
     * @return 意图列表的 JSON 数组字符串，用于填充到系统提示词模板中
     */
    private String buildIntentListPrompt(List<IntentConfig> intents) {
        JSONArray array = new JSONArray();
        for (IntentConfig intent : intents) {
            JSONObject obj = new JSONObject();
            obj.put("intentId", intent.getIntentId());
            obj.put("intentName", intent.getIntentName());
            obj.put("description", intent.getDescription());
            obj.put("matchPrompt", intent.getMatchPrompt());
            obj.put("examples", intent.getExampleQuestions());
            array.add(obj);
        }
        return array.toJSONString();
    }

    private IntentResult parseIntentResult(String response, List<IntentConfig> intents) {
                try {
                    String cleaned = response.trim()
                            .replaceAll("json", "") .replaceAll("", "")
                            .trim();
                    JSONObject json = JSON.parseObject(cleaned);
                    String intentId = json.getString("intentId");
                    double confidence = json.getDoubleValue("confidence");

                    if (intentId == null || intentId.isEmpty() || confidence < 0.7) {
                        return new IntentResult(null, "普通对话", null, confidence);
                    }

            return intents.stream()
                    .filter(i -> i.getIntentId().equals(intentId))
                    .findFirst()
                    .map(i -> new IntentResult(i.getIntentId(), i.getIntentName(), i.getWorkflowId(), confidence))
                    .orElse(new IntentResult(null, "普通对话", null, confidence));
        } catch (Exception e) {
            return new IntentResult(null, "普通对话", null, 0.0);
        }
    }
}
