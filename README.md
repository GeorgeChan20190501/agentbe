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

## 系统架构

```
                          ┌──────────────────────────────────────────────────────┐
                          │                    用户请求 (HTTP)                     │
                          └──────────────────────┬───────────────────────────────┘
                                                 │
                          ┌──────────────────────▼───────────────────────────────┐
                          │  ai-web          REST API 接口层                      │
                          │  ChatController  /ai/message  /ai/stream             │
                          └──────────────────────┬───────────────────────────────┘
                                                 │
                          ┌──────────────────────▼───────────────────────────────┐
                          │  ai-chat         聊天业务编排层                         │
                          │  ChatService     会话管理 / 历史加载 / 意图路由           │
                          │  ChatRepository  会话&消息持久化                        │
                          └───────┬──────────────────────────────┬───────────────┘
                                  │                              │
                                  │ 普通对话                       │  匹配到工作流
                                  ▼                              ▼
                ┌─────────────────────────┐    ┌─────────────────────────────────┐
                │  ai-intent              │    │  ai-workflow                    │
                │  意图识别                 │───▶│  工作流图引擎                     │
                │  IntentServiceImpl      │    │  WorkflowServiceImpl            │
                │  大模型语义匹配            │    │  条件分支/循环/并行                │
                └─────────────────────────┘    └────────┬───────────────┬────────┘
                                                        │               │
                                              ┌─────────▼──┐    ┌──────-▼──────────┐
                                              │  ai-tool   │    │  ai-skill       │
                                              │  工具执行    │    │  技能执行        │
                                              │  反射调用    │    │  提示词+大模型    │
                                              └────────────┘    └────────┬────────┘
                                                                         │
                                              ┌──────────────────────────▼────────┐
                                              │  ai-model                         │
                                              │  模型抽象层                         │
                                              │  AiModelFactory → QwenModelService│
                                              └───────────────────────────────────┘
```

## 请求处理流程

```
用户发消息 ──▶ ChatController (/ai/message 或 /ai/stream)
                    │
                    ▼
              ChatService.chat()
                    │
                    ├── 1. ensureSession()      确保会话存在
                    ├── 2. loadHistoryMessages() 加载最近10轮历史
                    ├── 3. buildContextQuestion() 拼接上下文问题
                    │
                    ▼
              IntentService.recognize()    意图识别
                    │
              ┌─────┴──────┐
              │            │
         匹配到意图    普通对话
              │            │
              ▼            ▼
       WorkflowService   AiModelFactory
       .execute()        .getDefaultModel()
              │            │
        ┌─────┴─────┐      │
        │           │      │
   tool节点     skill节点    │
        │           │      │
        ▼           ▼      │
   ToolExecutor  SkillService
   (反射调用)    (提示词模板  │
                  +大模型) │
        │           │     │
        └─────┬─────┘     │
              │           │
              ▼           ▼
           返回结果 / SSE流式响应
```

## 模块说明

| 模块 | 职责 | 核心类 | 依赖 |
|------|------|--------|------|
| **ai-common** | 基础公共层：实体、接口、上下文 | `AiWorkflowNode`, `WorkflowContext`, `ToolExecutor`, `AiChatMessage`, `AiChatSession` | MyBatis-Plus |
| **ai-start** | 启动入口：Spring Boot 自动配置 | `AiStartApplication` | ai-web, ai-common |
| **ai-web** | REST API 接口层 | `ChatController` | ai-chat |
| **ai-chat** | 聊天业务编排：会话管理、历史加载、意图路由 | `ChatService`, `ChatRepository` | ai-intent, ai-workflow, ai-model |
| **ai-intent** | 意图识别：大模型语义匹配，动态加载意图配置 | `IntentServiceImpl`, `IntentConfigLoader` | ai-common, Spring AI |
| **ai-workflow** | 工作流引擎：图遍历、条件分支(SpEL)、循环、并行 | `WorkflowServiceImpl` | ai-intent, ai-skill, ai-tool |
| **ai-skill** | 技能执行：加载DB配置、填充提示词模板、调用大模型 | `SkillServiceImpl` | ai-model, ai-tool |
| **ai-tool** | 工具执行：通过反射调用Spring Bean方法 | `ToolExecutorImpl` | ai-common |
| **ai-model** | 模型抽象：统一接口、工厂模式、多模型切换 | `AiModelService`, `AiModelFactory`, `QwenModelService` | Spring AI, DashScope |
| **ai-agent** | Agent（规划中） | — | — |
| **ai-system** | 系统管理（规划中） | — | — |
| **ai-admin** | 管理后台（规划中） | — | — |

## 模块依赖关系

```
ai-common          ← 基础公共层（实体、接口）
    ↑
    ├── ai-model        ← 模型抽象（Qwen已集成，DeepSeek/OpenAI预留）
    ├── ai-intent       ← 意图识别
    ├── ai-tool         ← 工具执行
    │     ↑
    │     └── ai-skill  ← 技能执行
    │           ↑
    │           └── ai-workflow  ← 工作流引擎
    │                 ↑
    └─────────────────┴── ai-chat      ← 聊天编排
                              ↑
                          ai-web       ← API接口
                              ↑
                          ai-start     ← 启动入口
```

## 数据库配置表

| 表名 | 用途 | 关联模块 |
|------|------|----------|
| `ai_chat_session` | 会话记录 | ai-chat |
| `ai_chat_message` | 消息记录 | ai-chat |
| `ai_intent` | 意图配置 | ai-intent |
| `ai_intent_example` | 意图示例问题 | ai-intent |
| `ai_workflow` | 工作流定义 | ai-workflow |
| `ai_workflow_node` | 工作流节点 | ai-workflow |
| `ai_skill` | 技能配置（提示词模板、模型参数） | ai-skill |
| `ai_tool` | 工具配置（bean_name + method_name） | ai-tool |
| `ai_model_config` | 模型参数配置 | ai-model |

## 技术栈

- **后端框架**: Spring Boot 3.5.15 + Spring Framework 6.2
- **AI集成**: Spring AI 1.1.4 + 阿里云DashScope（通义千问）
- **数据库**: MySQL + MyBatis-Plus 3.5.16
- **构建工具**: Maven (Java 17)
- **前端**: Vue 3 + Vite + TypeScript

## 演进路线

| 版本 | 目标 | 状态 |
|------|------|------|
| V1 | 简单智能问答 | ✅ 已完成 |
| V2 | 意图识别与专业回答 | ✅ 已完成 |
| V3 | 工作流编排（条件/循环/并行） | ✅ 已完成 |
| V4 | Tool工具调用 | ✅ 已完成 |
| V5 | 自主AI智能体（Agent） | 🔲 规划中 |
