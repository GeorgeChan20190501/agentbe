package com.icbc.agent.aiworkflow;

import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试SpEL条件表达式评估逻辑（对应WorkflowServiceImpl.evalCondition）
 */
class WorkflowConditionTest {

    private final ExpressionParser parser = new SpelExpressionParser();

    private boolean evalCondition(String expr, Map<String, Object> variables) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("vars", variables);

        String spelExpr = expr.trim();
        for (String key : variables.keySet()) {
            spelExpr = spelExpr.replace(key, "#vars['" + key + "']");
        }

        Boolean result = parser.parseExpression(spelExpr).getValue(ctx, Boolean.class);
        return result != null && result;
    }

    @Test
    void testEqualsCondition() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "VIP");
        assertTrue(evalCondition("status == 'VIP'", vars));
        assertFalse(evalCondition("status == 'normal'", vars));
    }

    @Test
    void testNotEqualsCondition() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "VIP");
        assertTrue(evalCondition("status != 'normal'", vars));
        assertFalse(evalCondition("status != 'VIP'", vars));
    }

    @Test
    void testGreaterThanCondition() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 5000);
        assertTrue(evalCondition("amount > 3000", vars));
        assertFalse(evalCondition("amount > 10000", vars));
    }

    @Test
    void testLessThanCondition() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 100);
        assertTrue(evalCondition("amount < 500", vars));
        assertFalse(evalCondition("amount < 50", vars));
    }

    @Test
    void testCompoundCondition() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 5000);
        vars.put("level", "gold");
        assertTrue(evalCondition("amount > 3000 && level == 'gold'", vars));
        assertFalse(evalCondition("amount > 10000 && level == 'gold'", vars));
    }

    @Test
    void testBooleanVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("isActive", true);
        assertTrue(evalCondition("isActive", vars));
    }

    @Test
    void testChineseVariableName() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("订单金额", 8000);
        assertTrue(evalCondition("订单金额 > 5000", vars));
    }

    @Test
    void testNullExpression() {
        Map<String, Object> vars = new HashMap<>();
        assertFalse(evalCondition("", vars));
    }
}
