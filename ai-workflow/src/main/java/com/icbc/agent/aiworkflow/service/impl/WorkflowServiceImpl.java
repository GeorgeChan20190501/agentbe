package com.icbc.agent.aiworkflow.service.impl;

import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiworkflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;
    private final SkillExecutor skillExecutor;


    @Override
    public String execute(IntentResult result) {
        if (result.isNormalChat()) {
            return null;
        }

        List<Map<String, Object>> nodes = loadWorkflowNodes(result.getWorkflowId());
        if (nodes.isEmpty()) {
            return "工作流未配置执行节点";
        }

        Map<String, Object> context = new HashMap<>();
        StringBuilder resultBuilder = new StringBuilder();

        for (Map<String, Object> node : nodes) {
            String nodeType = (String) node.get("node_type");
            String refId = (String) node.get("ref_id");
            String nodeName = (String) node.get("node_name");

            log.info("执行工作流节点: {} (类型: {}, refId: {})", nodeName, nodeType, refId);

            try {
                if ("tool".equals(nodeType)) {
                    Object toolResult = executeTool(refId, context);
                    context.put(nodeName, toolResult);
                } else if ("skill".equals(nodeType)) {
                    String skillResult = executeSkill(refId, context);
                    resultBuilder.append(skillResult);
                }
            } catch (Exception e) {
                log.error("节点执行失败: {} - {}", nodeName, e.getMessage(), e);
                resultBuilder.append("节点[").append(nodeName).append("]执行失败: ").append(e.getMessage());
            }
        }

        return resultBuilder.toString();
    }

    @Override
    public Flux<Object> executeStream(IntentResult result) {
        // 如果是正常聊天，则不执行任何操作
        if (result.isNormalChat()) {
            return Flux.empty();
        }

        // 根据识别到的意图，加载工作流节点
        List<Map<String, Object>> nodes = loadWorkflowNodes(result.getWorkflowId());
        if (nodes.isEmpty()) {
            return Flux.just("工作流未配置执行节点");
        }

        Map<String, Object> context = new HashMap<>();

        for (Map<String, Object> node : nodes) {
            String nodeType = (String) node.get("node_type");
            String refId = (String) node.get("ref_id");
            String nodeName = (String) node.get("node_name");

            if ("tool".equals(nodeType)) {
                try {
                    Object toolResult = executeTool(refId, context);
                    context.put(nodeName, toolResult);
                } catch (Exception e) {
                    log.error("工具节点执行失败: {}", nodeName, e);
                }
            } else if ("skill".equals(nodeType)) {
                return skillExecutor.executeStream(refId, context);
            }
        }

        return Flux.just("工作流未包含技能节点");
    }

    private Object executeTool(String toolId, Map<String, Object> context) throws Exception {
        String sql = "SELECT bean_name, method_name FROM ai_tool WHERE tool_id = ? AND enable_flag = '是'";
        Map<String, Object> tool = jdbcTemplate.queryForMap(sql, toolId);

        String beanName = (String) tool.get("bean_name");
        String methodName = (String) tool.get("method_name");

        Object bean = applicationContext.getBean(beanName);

        Method method = findMethod(bean, methodName);

        Object[] args = resolveToolArgs(method, context);
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

    private Object[] resolveToolArgs(Method method, Map<String, Object> context) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == String.class) {
                args[i] = context.getOrDefault("defaultParam", "1001");
            }
        }
        return args;
    }

    private String executeSkill(String skillId, Map<String, Object> context) {
        return skillExecutor.execute(skillId, context);
    }

    private List<Map<String, Object>> loadWorkflowNodes(String workflowId) {
        String sql = "SELECT node_type, node_name, ref_id " +
                "FROM ai_workflow_node WHERE workflow_id = ? AND enable_flag = '是' ORDER BY sort_no";
        return jdbcTemplate.queryForList(sql, workflowId);
    }
}
