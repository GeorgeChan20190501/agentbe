package com.icbc.agent.aiworkflow.service.impl;

import com.icbc.agent.aicommon.chat.bean.AiWorkflowNode;
import com.icbc.agent.aicommon.chat.bean.ToolExecutor;
import com.icbc.agent.aicommon.chat.bean.WorkflowContext;
import com.icbc.agent.aiintent.entity.IntentResult;
import com.icbc.agent.aiskill.service.SkillService;
import com.icbc.agent.aiworkflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    private final ToolExecutor toolExecutor;
    private final SkillService skillService;

    // ==================== 公共接口 ====================

    @Override
    public String execute(IntentResult result) {
        if (result.isNormalChat()) {
            return null;
        }

        Map<String, AiWorkflowNode> nodeMap = loadWorkflowNodeMap(result.getWorkflowId());
        if (nodeMap.isEmpty()) return "工作流未配置执行节点";

        WorkflowContext context = buildContext(result);
        AiWorkflowNode current = findEntryNode(nodeMap);
        if (current == null) {
            return "工作流未找到入口节点";
        }

        // 同步执行工作流图中的各个节点，执行结果保存到context中
        int maxIterations = 1000;
        while (current != null && maxIterations-- > 0) {
            if ("end".equals(current.getNodeType())) break;
            if (!executeNodeSync(current, nodeMap, context)) break;
            current = advanceNode(current, nodeMap, context);
        }
        // 返回工作流执行结果
        return context.getResult() != null ? context.getResult() : "";
    }

    @Override
    public Flux<Object> executeStream(IntentResult result) {
        if (result.isNormalChat()) {
            return Flux.empty();
        }

        //获取数据库配置的工作流节点信息
        Map<String, AiWorkflowNode> nodeMap = loadWorkflowNodeMap(result.getWorkflowId());
        if (nodeMap.isEmpty()) {
            return Flux.just("工作流未配置执行节点");
        }
        //构建工作流执行上下文
        WorkflowContext context = buildContext(result);
        //找到工作流的入口节点
        AiWorkflowNode current = findEntryNode(nodeMap);
        if (current == null) {
            return Flux.just("工作流未找到入口节点");
        }

        //执行工作流图
        return executeStreamGraph(current, nodeMap, context);
    }

    // ==================== 核心执行引擎 ====================

    private WorkflowContext buildContext(IntentResult result) {
        WorkflowContext context = new WorkflowContext();
        context.setSessionId(result.getSessionId());
        context.setQuestion(result.getQuestion());
        context.setWorkflowId(result.getWorkflowId());
        context.setIntentId(result.getIntentId());
        return context;
    }

    /**
     * 同步执行单个节点，返回是否继续执行下一个节点
     */
    private boolean executeNodeSync(AiWorkflowNode node, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        try {
            log.info("执行节点: {} (类型: {})", node.getNodeName(), node.getNodeType());
            switch (node.getNodeType()) {
                case "tool":
                    Object toolResult = toolExecutor.execute(node.getRefId(), node.getNodeConfig(), context);
                    context.put(node.getNodeName(), toolResult);
                    return true;
                case "skill":
                    context.setResult(skillService.execute(node.getRefId(), context));
                    return true;
                case "parallel":
                    executeParallelNode(node, nodeMap, context);
                    return true;
                default:
                    return true;
            }
        } catch (Exception e) {
            log.error("节点执行失败: {}", node.getNodeName(), e);
            context.setResult("节点[" + node.getNodeName() + "]执行失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据节点类型计算下一个要执行的节点
     */
    private AiWorkflowNode advanceNode(AiWorkflowNode current, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        switch (current.getNodeType()) {
            case "if":
                boolean condition = evalCondition(current.getConditionExpr(), context);
                String branchId = condition ? current.getTrueNodeId() : current.getFalseNodeId();
                return branchId != null ? nodeMap.get(branchId) : null;
            case "loop":
                if (evalCondition(current.getConditionExpr(), context)) {
                    AiWorkflowNode loopBody = nodeMap.get(current.getLoopBodyNodeId());
                    if (loopBody != null) return loopBody;
                }
                return current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
            case "end":
                return null;
            default:
                return current.getNextNodeId() != null ? nodeMap.get(current.getNextNodeId()) : null;
        }
    }

    /**
     * 流式执行图：非skill节点同步执行，遇到skill节点返回Flux流
     */
    private Flux<Object> executeStreamGraph(AiWorkflowNode current, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        while (current != null) {
            log.info("执行流式节点: {} (类型: {})", current.getNodeName(), current.getNodeType());
            try {
                if ("skill".equals(current.getNodeType())) {
                    //读取数据库配置的skill信息,将工具执行结果填充到提示词模板，结合配置的skill系统提示词，形成最终的提示词，让大模型生成结果
                    return skillService.executeStream(current.getRefId(), context);
                }
                if ("end".equals(current.getNodeType())) {
                    return Flux.just(context.getResult() != null ? context.getResult() : "");
                }
                if (!executeNodeSync(current, nodeMap, context)) {
                    return Flux.error(new RuntimeException("节点执行失败: " + current.getNodeName()));
                }
                current = advanceNode(current, nodeMap, context);
            } catch (Exception e) {
                log.error("流式节点执行失败: {}", current.getNodeName(), e);
                return Flux.error(e);
            }
        }
        return Flux.just("工作流执行完成");
    }

    // ==================== 并行节点 ====================

    /**
     * 并行节点执行：各分支独立执行，完成后将分支新增的变量合并回主context
     */
    private void executeParallelNode(AiWorkflowNode node, Map<String, AiWorkflowNode> nodeMap, WorkflowContext context) {
        if (node.getNodeConfig() == null || node.getNodeConfig().isEmpty()) {
            log.warn("并行节点未配置分支: {}", node.getNodeName());
            return;
        }

        String[] branchNodeIds = node.getNodeConfig().split(",");
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (String branchNodeId : branchNodeIds) {
            AiWorkflowNode branchNode = nodeMap.get(branchNodeId.trim());
            if (branchNode != null) {
                Map<String, Object> branchVarSnapshot = new ConcurrentHashMap<>(context.getVariables());
                futures.add(CompletableFuture.supplyAsync(() -> {
                    WorkflowContext branchContext = new WorkflowContext();
                    branchContext.setSessionId(context.getSessionId());
                    branchContext.setQuestion(context.getQuestion());
                    branchContext.setWorkflowId(context.getWorkflowId());
                    branchContext.setIntentId(context.getIntentId());
                    branchContext.setVariables(branchVarSnapshot);
                    // 内联同步执行分支图
                    AiWorkflowNode cur = branchNode;
                    int maxIter = 1000;
                    while (cur != null && maxIter-- > 0) {
                        if ("end".equals(cur.getNodeType())) break;
                        executeNodeSync(cur, nodeMap, branchContext);
                        cur = advanceNode(cur, nodeMap, branchContext);
                    }
                    return branchContext.getVariables();
                }));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 合并所有分支的变量变更回主context（仅合并新增的key，不覆盖已有值）
        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> branchVars = future.get();
                for (Map.Entry<String, Object> entry : branchVars.entrySet()) {
                    if (!context.getVariables().containsKey(entry.getKey())) {
                        context.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.error("并行分支执行失败", e);
            }
        }
    }

    // ==================== 数据加载 ====================

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

    // ==================== 条件表达式引擎 (SpEL) ====================

    /**
     * 使用Spring EL表达式引擎评估条件，支持 ==, !=, >, <, >=, <=, &&, || 等运算符
     * 上下文变量通过 #vars['key'] 访问，支持中文变量名
     */
    private boolean evalCondition(String expr, WorkflowContext context) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        try {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("vars", context.getVariables());

            // 将裸变量名转换为 #vars['name'] 格式，支持中文和英文变量名
            String spelExpr = expr.trim();
            for (String key : context.getVariables().keySet()) {
                spelExpr = spelExpr.replace(key, "#vars['" + key + "']");
            }

            Boolean result = parser.parseExpression(spelExpr).getValue(evalContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            log.warn("条件表达式评估失败: {}", expr, e);
            return false;
        }
    }
}
