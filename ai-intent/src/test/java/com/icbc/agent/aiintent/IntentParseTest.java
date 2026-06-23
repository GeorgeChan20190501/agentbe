package com.icbc.agent.aiintent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试意图识别结果解析逻辑（对应IntentServiceImpl.parseIntentResult的核心解析）
 */
class IntentParseTest {

    private String cleanResponse(String response) {
        return response.trim()
                .replaceAll("json", "")
                .replaceAll("", "")
                .trim();
    }

    @Test
    void testParseNormalResponse() {
        String response = "{\"intentId\":\"query_order\",\"confidence\":0.95}";
        String cleaned = cleanResponse(response);
        JSONObject json = JSON.parseObject(cleaned);

        assertEquals("query_order", json.getString("intentId"));
        assertEquals(0.95, json.getDoubleValue("confidence"));
        assertTrue(json.getDoubleValue("confidence") >= 0.7);
    }

    @Test
    void testParseLowConfidenceResponse() {
        String response = "{\"intentId\":\"query_order\",\"confidence\":0.3}";
        String cleaned = cleanResponse(response);
        JSONObject json = JSON.parseObject(cleaned);

        assertEquals("query_order", json.getString("intentId"));
        assertTrue(json.getDoubleValue("confidence") < 0.7, "低置信度应被过滤");
    }

    @Test
    void testParseEmptyIntentId() {
        String response = "{\"intentId\":\"\",\"confidence\":0.5}";
        String cleaned = cleanResponse(response);
        JSONObject json = JSON.parseObject(cleaned);

        String intentId = json.getString("intentId");
        assertTrue(intentId == null || intentId.isEmpty(), "空intentId应视为普通对话");
    }

    @Test
    void testParseResponseWithMarkdown() {
        String response = "```json\n{\"intentId\":\"test\",\"confidence\":0.85}\n```";
        String cleaned = cleanResponse(response);
        JSONObject json = JSON.parseObject(cleaned);

        assertEquals("test", json.getString("intentId"));
        assertEquals(0.85, json.getDoubleValue("confidence"));
    }

    @Test
    void testParseInvalidResponse() {
        String response = "这不是JSON格式";
        assertThrows(Exception.class, () -> {
            String cleaned = cleanResponse(response);
            JSON.parseObject(cleaned);
        }, "非法JSON应抛出异常");
    }
}
