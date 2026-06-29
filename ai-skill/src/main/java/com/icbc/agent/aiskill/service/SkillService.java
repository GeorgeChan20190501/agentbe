package com.icbc.agent.aiskill.service;

import com.icbc.agent.aicommon.chat.bean.WorkflowContext;
import reactor.core.publisher.Flux;

/**
 * 技能服务接口
 * 负责加载技能配置、填充提示词模板、调用大模型生成结果
 */
public interface SkillService {

    /**
     * 同步执行技能
     */
    String execute(String skillId, WorkflowContext context);

    /**
     * 流式执行技能
     */
    Flux<Object> executeStream(String skillId, WorkflowContext context);
}
