# agentbe
持续演进的AI智能体后端

              用户聊天               定时任务入口         事件入口
                 ↓                      ↓                ↓
              Chat入口                Agent            Agent
                 ↓                              ↓
              Intent                         Planner     
                 ↓                              ↓
      （固定workflow 或 Planner）           WorkflowEngine
                 ↓                              ↓
      -------------------------------------------------------------------- 
                             ↓         ↓
                         Tool节点   Skill节点
                             ↓         ↓
                                Model
                                  ↓
                                Qwen


项目架构分析 

项目采用了清晰的微服务架构：<br>
后端 agentbe 包含12个模块（ai-admin, ai-agent, ai-chat, ai-common, ai-intent, ai-model, ai-skill, ai-start, ai-system, ai-tool, ai-web, ai-workflow）
前端 agentfe 使用Vue + Vite技术栈<br>
V1-V5演进路线评估

你的演进路线非常合理，符合业界标准：<br>
V1 - 简单智能问答 ✅ 基础能力，验证模型可用性<br>
V2 - 意图识别与专业回答 ✅ 核心AI能力增强<br>
V3 - 工作流编排 ✅ 业务流程自动化<br>
V4 - Tool工具调用 ✅ 实际业务集成<br>
V5 - 自主AI智能体 ✅ 最高级别AI自主决策<br>

这个路线图完全符合大厂AI智能体的发展路径，从简单到复杂，逐步构建更高级的能力。
可行性评估<br>
✅ 技术可行性高 - 后端模块划分合理，支持渐进式开发 <br>
✅ 架构设计优秀 - 模块化设计便于功能扩展<br> 
✅ 演进路径清晰 - 每个版本都有明确的目标和价值<br>
你的项目有很大潜力成功！这正是当前AI领域最前沿的方向。

ai-common    ← 基础公共层<br>
ai-start     ← 启动入口<br>
ai-web       ← 接收请求<br>
ai-chat      ← 聊天处理<br>
ai-intent    ← 意图识别<br>
ai-workflow  ← 工作流编排<br>
ai-skill     ← 技能<br>
ai-tool      ← 工具<br>
ai-model     ← 模型汇总总结<br>
ai-agent     ← Agent<br>
ai-system    ← 系统<br>
ai-admin     ← 管理<br>