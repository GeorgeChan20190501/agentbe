package com.icbc.agent.aitool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.icbc.agent.aicommon.chat.bean.ToolExecutor;
import com.icbc.agent.aicommon.chat.bean.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行器实现
 * 通过反射机制调用数据库中配置的Tool Bean方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutorImpl implements ToolExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;

    @Override
    public Object execute(String toolId, String nodeConfig, WorkflowContext context) throws Exception {
        String sql = "SELECT bean_name, method_name FROM ai_tool WHERE tool_id = ? AND enable_flag = '是'";
        Map<String, Object> tool = jdbcTemplate.queryForMap(sql, toolId);

        String beanName = (String) tool.get("bean_name");
        String methodName = (String) tool.get("method_name");

        Object bean = applicationContext.getBean(beanName);
        Method method = findMethod(bean, methodName);
        Object[] args = resolveToolArgs(method, nodeConfig, context);
        return method.invoke(bean, args);
    }

    private Method findMethod(Object bean, String methodName) throws NoSuchMethodException {
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        throw new NoSuchMethodException("方法不存在: " + methodName);
    }

    /**
     * 解析Tool参数：优先从nodeConfig的argMappings读取，否则按参数类型尝试从context匹配
     * nodeConfig格式示例: {"argMappings":{"0":"question","1":"customerId"}}
     */
    private Object[] resolveToolArgs(Method method, String nodeConfig, WorkflowContext context) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        Map<String, String> argMappings = null;
        if (nodeConfig != null && !nodeConfig.isEmpty()) {
            try {
                JSONObject config = JSON.parseObject(nodeConfig);
                JSONObject mappings = config.getJSONObject("argMappings");
                if (mappings != null) {
                    argMappings = new HashMap<>();
                    for (String key : mappings.keySet()) {
                        argMappings.put(key, mappings.getString(key));
                    }
                }
            } catch (Exception e) {
                log.warn("Tool nodeConfig解析失败: {}", nodeConfig, e);
            }
        }

        for (int i = 0; i < paramTypes.length; i++) {
            if (argMappings != null && argMappings.containsKey(String.valueOf(i))) {
                String contextKey = argMappings.get(String.valueOf(i));
                args[i] = context.get(contextKey);
            } else if (paramTypes[i] == String.class) {
                args[i] = context.getOrDefault("defaultParam", "1001");
            }
        }
        return args;
    }
}
