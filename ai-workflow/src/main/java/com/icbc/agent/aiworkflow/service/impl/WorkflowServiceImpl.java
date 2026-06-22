package com.icbc.agent.aiworkflow.service.impl;

import com.icbc.agent.aicommon.chat.bean.AiWorkflowNode;
import com.icbc.agent.aicommon.chat.bean.WorkflowContext;
import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiworkflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

        Map<String, AiWorkflowNode> nodeMap = loadWorkflowNodeMap(result.getWorkflowId());
        if (nodeMap.isEmpty()) {
            return "工作流未配置执行节点";
        }

        WorkflowContext context = new WorkflowContext();
        context.setSessionId(result.getSessionId());
        context.setQuestion(result.getQuestion());
        context.setWorkflowId(result.getWorkflowId());
        context.setIntentId(result.getIntentId());

        AiWorkflowNode entryNode = findEntryNode(nodeMap);
        if (entryNode == null) {
            return "工作流未找到入口节点";
        }

        executeGraph(entryNode, nodeMap, context);
        return context.getResult() != null ? context.getResult() : "";
    }

    @Override
    public Flux<Object> executeStream(IntentResult result) {
        if (result.isNormalChat()) {
            return Flux.empty();
        }

        Map<String, AiWorkflowNode> nodeMap = loadWorkflowNodeMap(result.getWorkflowId());
        if (nodeMap.isEmpty()) {
            return Flux.just("工作流未配置执行节点");
        }

        WorkflowContext context = new WorkflowContext();
        context.setSessionId(result.getSessionId());
        context.setQuestion(result.getQuestion());
        context.setWorkflowId(result.getWorkflowId());
        context.setIntentId(result.getIntentId());

        AiWorkflowNode entryNode = findEntryNode(nodeMap);
        if (entryNode == null) {
            return Flux.just("工作流未找到入口节点");
        }

        return executeStreamGraph(entryNode, nodeMap, context);
    }

    private Object executeTool(String toolId, WorkflowContext context) throws Exception {
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

    private Object[] resolveToolArgs(Method method, WorkflowContext context) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == String.class) {
                args[i] = context.getOrDefault("defaultParam", "1001");
            }
        }
        return args;
    }

    private String executeSkill(String skillId, WorkflowContext context) {
        return skillExecutor.execute(skillId, context);
    }

    private Map<String, AiWorkflowNode> loadWorkflowNodeMap(String workflowId) {
        String sql = "SELECT id, node_type, node_name, next_node_id, true_node_id, " +
                "false_node_id, condition_expr, loop_body_node_id, ref_id, sort_no " +
                "FROM ai_workflow_node WHERE workflow_id = ? AND enable_flag = '是' ORDER BY sort_no";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, workflowId);
        Map<String, AiWorkflowNode> nodeMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            AiWorkflowNode node = new AiWorkflowNode();
            node.setId((Integer) row.get("id"));
            node.setNodeType((String) row.get("node_type"));
            node.setNodeName((String) row.get("node_name"));
            node.setNextNodeId((String) row.get("next_node_id"));
            node.setTrueNodeId((String) row.get("true_node_id"));
            node.setFalseNodeId((String) row.get("false_node_id"));
            node.setConditionExpr((String) row.get("condition_expr"));
            node.setLoopBodyNodeId((String) row.get("loop_body_node_id"));
            node.setRefId((String) row.get("ref_id"));
            node.setNodeConfig((String) row.get("node_config"));
            node.setSortNo((Integer) row.get("sort_no"));
            nodeMap.put(String.valueOf(node.getId()), node);
        }
        return nodeMap;
    }

    private AiWorkflowNode findEntryNode(Map<String, AiWorkflowNode> nodeMap) {
        return nodeMap.values().stream()
                .min((a, b) -> Integer.compare(a.getSortNo() != null ? a.getSortNo() : 0, 
                                                b.getSortNo() != null ? b.getSortNo() : 0))
                .orElse(null);
    }

    private void executeGraph(AiWorkflowNode current, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        int maxIterations = 1000;
        int iteration = 0;
        
        while (current != null && iteration++ < maxIterations) {
            log.info("执行节点: {} (类型: {})", current.getNodeName(), current.getNodeType());
            
            try {
                switch (current.getNodeType()) {
                    case "tool":
                        Object toolResult = executeTool(current.getRefId(), context);
                        context.put(current.getNodeName(), toolResult);
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    case "skill":
                        String skillResult = executeSkill(current.getRefId(), context);
                        context.setResult(skillResult);
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    case "end":
                        return;
                        
                    case "if":
                        boolean condition = evalCondition(current.getConditionExpr(), context);
                        String nextId = condition ? current.getTrueNodeId() : current.getFalseNodeId();
                        current = nextId != null ? nodeMap.get(nextId) : null;
                        break;
                        
                    case "parallel":
                        executeParallelNode(current, nodeMap, context);
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    case "loop":
                        AiWorkflowNode loopBody = nodeMap.get(current.getLoopBodyNodeId());
                        while (evalCondition(current.getConditionExpr(), context) && loopBody != null) {
                            executeGraph(loopBody, nodeMap, context);
                        }
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    default:
                        log.warn("未知节点类型: {}", current.getNodeType());
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                }
            } catch (Exception e) {
                log.error("节点执行失败: {}", current.getNodeName(), e);
                context.setResult("节点[" + current.getNodeName() + "]执行失败: " + e.getMessage());
                return;
            }
        }
    }

    private Flux<Object> executeStreamGraph(AiWorkflowNode current, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        while (current != null) {
            log.info("执行流式节点: {} (类型: {})", current.getNodeName(), current.getNodeType());
            
            try {
                switch (current.getNodeType()) {
                    case "tool":
                        Object toolResult = executeTool(current.getRefId(), context);
                        context.put(current.getNodeName(), toolResult);
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    case "skill":
                        return skillExecutor.executeStream(current.getRefId(), context);
                        
                    case "end":
                        return Flux.just(context.getResult() != null ? context.getResult() : "");
                        
                    case "if":
                        boolean condition = evalCondition(current.getConditionExpr(), context);
                        String nextId = condition ? current.getTrueNodeId() : current.getFalseNodeId();
                        current = nextId != null ? nodeMap.get(nextId) : null;
                        break;
                        
                    case "parallel":
                        executeParallelNode(current, nodeMap, context);
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    case "loop":
                        AiWorkflowNode loopBody = nodeMap.get(current.getLoopBodyNodeId());
                        while (evalCondition(current.getConditionExpr(), context) && loopBody != null) {
                            executeGraph(loopBody, nodeMap, context);
                        }
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                        break;
                        
                    default:
                        log.warn("未知节点类型: {}", current.getNodeType());
                        current = current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
                }
            } catch (Exception e) {
                log.error("流式节点执行失败: {}", current.getNodeName(), e);
                return Flux.error(e);
            }
        }
        return Flux.just("工作流执行完成");
    }

    private void executeParallelNode(AiWorkflowNode node, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        if (node.getNodeConfig() == null || node.getNodeConfig().isEmpty()) {
            log.warn("并行节点未配置分支: {}", node.getNodeName());
            return;
        }
        
        String[] branchNodeIds = node.getNodeConfig().split(",");
        List<CompletableFuture<Object>> futures = new ArrayList<>();
        
        for (String branchNodeId : branchNodeIds) {
            AiWorkflowNode branchNode = nodeMap.get(branchNodeId.trim());
            if (branchNode != null) {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                    WorkflowContext branchContext = new WorkflowContext();
                    branchContext.setSessionId(context.getSessionId());
                    branchContext.setQuestion(context.getQuestion());
                    branchContext.setWorkflowId(context.getWorkflowId());
                    branchContext.setIntentId(context.getIntentId());
                    branchContext.setVariables(new ConcurrentHashMap<>(context.getVariables()));
                    executeGraph(branchNode, nodeMap, branchContext);
                    return branchContext.getResult();
                });
                futures.add(future);
            }
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        for (CompletableFuture<Object> future : futures) {
            try {
                Object result = future.get();
                if (result != null) {
                    log.info("并行分支执行结果: {}", result);
                }
            } catch (Exception e) {
                log.error("并行分支执行失败", e);
            }
        }
    }

    private boolean evalCondition(String expr, WorkflowContext context) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            Object left = context.get(parts[0].trim());
            String right = parts[1].trim().replace("\"", "").replace("'", "");
            return !right.equals(String.valueOf(left));
        } else if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            Object left = context.get(parts[0].trim());
            String right = parts[1].trim().replace("\"", "").replace("'", "");
            return right.equals(String.valueOf(left));
        } else if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            Object left = context.get(parts[0].trim());
            try {
                double leftVal = Double.parseDouble(String.valueOf(left));
                double rightVal = Double.parseDouble(parts[1].trim());
                return leftVal > rightVal;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            Object left = context.get(parts[0].trim());
            try {
                double leftVal = Double.parseDouble(String.valueOf(left));
                double rightVal = Double.parseDouble(parts[1].trim());
                return leftVal < rightVal;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            Object value = context.get(expr);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value != null) {
                return !String.valueOf(value).isEmpty() && !"false".equalsIgnoreCase(String.valueOf(value));
            }
            return false;
        }
    }
}
