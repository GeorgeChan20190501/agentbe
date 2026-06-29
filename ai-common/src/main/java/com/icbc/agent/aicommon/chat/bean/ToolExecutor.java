package com.icbc.agent.aicommon.chat.bean;

/**
 * 工具执行器接口
 * 由 ai-tool 模块实现，负责通过反射调用工具方法
 */
public interface ToolExecutor {

    /**
     * 执行工具方法
     * @param toolId     工具ID（对应ai_tool表）
     * @param nodeConfig 节点配置（JSON格式的参数映射）
     * @param context    工作流执行上下文
     * @return 工具执行结果
     */
    Object execute(String toolId, String nodeConfig, WorkflowContext context) throws Exception;
}
