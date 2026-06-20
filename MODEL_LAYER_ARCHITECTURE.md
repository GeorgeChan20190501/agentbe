# AI模型层架构设计

## 架构分层

```
┌─────────────────────────────────────┐
│         ai-web (Controller)         │  ← REST API接口层
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        ai-chat (Service)            │  ← 业务逻辑层
│     调用ai-model  不直接依赖Spring AI│
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       ai-model (Model Layer)        │  ← 模型抽象层
│  - AiModelService (接口)            │
│  - AiModelFactory (工厂)            │
│  - QwenModelService (千问实现)      │
│  - DeepSeekModelService (预留)      │
│  - OpenAIModelService (预留)        │
└─────────────────────────────────────┘
```

## 核心设计

### 1. 模型接口 (AiModelService)
定义统一的对话接口，所有模型实现都必须遵循此接口。

```java
public interface AiModelService {
    String chat(String message);
    String getModelName();
}
```

### 2. 模型工厂 (AiModelFactory)
根据配置动态选择模型实现，支持运行时切换。

**配置方式：**
```properties
# application.properties
ai.model.provider=qwen  # 可切换为 deepseek, openai等
```

**使用方式：**
```java
// 获取默认模型
AiModelService model = modelFactory.getDefaultModel();

// 获取指定模型
AiModelService model = modelFactory.getModel("deepseek");
```

### 3. 模型实现
每个模型提供者一个实现类，使用 `@Service("xxxModel")` 注解注册。

**命名规范：**
- Bean名称：`{provider}Model` （如：qwenModel, deepseekModel）
- 类名：`{Provider}ModelService` （如：QwenModelService）

## 当前支持的模型

### ✅ 通义千问 (Qwen)
- **Bean名称**: `qwenModel`
- **状态**: 已集成
- **配置**: 
  ```properties
  ai.model.provider=qwen
  spring.ai.dashscope.api-key=your-api-key
  ```

### 🔲 DeepSeek (预留)
- **Bean名称**: `deepseekModel`
- **状态**: 待集成
- **文件**: `DeepSeekModelService.java` (已注释@Service)
- **集成步骤**:
  1. 在父pom.xml添加 `spring-ai-starter-model-deepseek`
  2. 取消 `@Service("deepseekModel")` 注释
  3. 实现chat方法
  4. 配置API Key

### 🔲 OpenAI (预留)
- **Bean名称**: `openaiModel`
- **状态**: 待集成
- **文件**: `OpenAIModelService.java` (已注释@Service)
- **集成步骤**: 同DeepSeek

## 如何切换到新模型

### 方式1: 配置文件切换（推荐）
只需修改一行配置：
```properties
ai.model.provider=deepseek  # 从qwen切换到deepseek
```

### 方式2: 代码中指定
```java
// 使用默认模型
chatService.chat("你好");

// 使用指定模型
chatService.chat("你好", "deepseek");
```

## 集成新模型的步骤

以集成DeepSeek为例：

### 1. 添加依赖 (父pom.xml)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>
```

### 2. 启用服务类
```java
@Service("deepseekModel")  // 取消注释
public class DeepSeekModelService implements AiModelService {
    // ...
}
```

### 3. 实现chat方法
```java
@Override
public String chat(String message) {
    return chatClient.prompt()
            .user(message)
            .call()
            .content();
}
```

### 4. 配置API Key
```properties
spring.ai.deepseek.api-key=${DEEPSEEK_API_KEY}
spring.ai.deepseek.chat.options.model=deepseek-chat
```

### 5. 切换模型
```properties
ai.model.provider=deepseek
```

## 扩展性设计

### 支持多模型并发
```java
// 可以同时使用多个模型
String qwenResponse = chatService.chat("你好", "qwen");
String deepseekResponse = chatService.chat("你好", "deepseek");
```

### 模型对比测试
```java
// 对比不同模型的回答
Map<String, String> responses = new HashMap<>();
for (String provider : Arrays.asList("qwen", "deepseek", "openai")) {
    try {
        responses.put(provider, chatService.chat(question, provider));
    } catch (Exception e) {
        responses.put(provider, "Error: " + e.getMessage());
    }
}
```

### 动态加载模型
未来可以通过SPI机制或插件化方式动态加载新模型，无需修改核心代码。

## 优势

1. **解耦**: ai-chat层不直接依赖Spring AI，只依赖ai-model接口
2. **灵活**: 通过配置即可切换模型，无需改代码
3. **可扩展**: 新增模型只需实现接口，符合开闭原则
4. **可测试**: 可以轻松Mock模型进行单元测试
5. **向后兼容**: 旧代码无需修改，平滑迁移

## 注意事项

1. **Bean命名**: 必须遵循 `{provider}Model` 命名规范
2. **异常处理**: 未集成的模型会抛出 `UnsupportedOperationException`
3. **依赖管理**: Spring AI相关依赖只在ai-model层引入
4. **配置隔离**: 每个模型的配置独立，互不影响


```sql
 create table if not exists ai_chat_session
(
    id               int auto_increment  primary key,
    session_id       varchar(50) default '' unique comment '会话ID，作为唯一标识',
    user_id          varchar(20)  default ''  comment '用户ID',
    session_name     varchar(200) default ''  comment '会话名称',
    last_message     varchar(100)  default '' comment '最后一条消息',
    message_count    int   default 0 comment '消息数量',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_user_id (user_id),
    key idx_session_name (session_name)
) comment='消息会话表';

create table if not exists ai_chat_message
(
    id               int auto_increment  primary key,
    session_id       varchar(50)  default ''  comment '会话ID',
    role             varchar(50)  default ''  comment '角色：用户、系统、助手',
    content          TEXT    comment '内容',
    content_html     TEXT    comment '内容HTML',
    intent_id        varchar(50)  default ''  comment '意图ID',
    workflow_id      varchar(50)  default ''  comment '工作流ID',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_session_id (session_id),
    key idx_intent_id (intent_id),
    key idx_workflow_id (workflow_id)
) comment='对话消息详细表';

-- 可用Redis保持近20轮对话，直接读取，当前会话结束后，异步落库


create table if not exists ai_chat_context
(
    id               int auto_increment  primary key,
    session_id       varchar(50)  default ''  comment '会话ID',
    role             varchar(50)  default ''  comment '角色：用户、系统、助手',
    content          TEXT    comment '内容',
    content_html     TEXT    comment '内容HTML',
    intent_id        varchar(50)  default ''  comment '意图ID',
    workflow_id      varchar(50)  default ''  comment '工作流ID',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_session_id (session_id),
    key idx_intent_id (intent_id),
    key idx_workflow_id (workflow_id)
) comment='对话消息详细表(长期记忆)';


create table if not exists ai_intent
(
    id               int auto_increment  primary key,
    intent_id       varchar(50)  default ''  comment '意图ID',
    intent_name     varchar(50)  default ''  comment '意图名称',
    description     varchar(200) default ''  comment '意图描述',
    match_prompt     varchar(500)  default '' comment '匹配提示词',
    workflow_id      varchar(50)  default ''  comment '工作流ID',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    sort_no          int   default 0 comment '排序编号',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_intent_id (intent_id),
    key idx_intent_name (intent_name)
) comment='意图识别表';


create table if not exists ai_intent_example
(
    id               int auto_increment  primary key,
    intent_id        varchar(50)  default ''  comment '意图ID',
    question         varchar(500)  default ''  comment '示例问题',
    answer         varchar(5000)  default ''  comment '示例答案',
    key idx_intent_id (intent_id),
    key idx_question (question)
) comment='意图识别示例表';

create table if not exists ai_workflow
(
    id               int auto_increment  primary key,
    workflow_id      varchar(50)  default ''  comment '工作流ID',
    workflow_name    varchar(50)  default ''  comment '工作流名称',
    description      varchar(200) default ''  comment '工作流描述',
    workflow_json    TEXT    comment '工作流JSON',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    sort_no          int   default 0 comment '排序编号',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_workflow_id (workflow_id),
    key idx_workflow_name (workflow_name)
) comment='工作流表';

create table if not exists ai_workflow_node
(
    id               int auto_increment  primary key,
    workflow_id      varchar(50)  default ''  comment '工作流ID',
    node_type        varchar(50)  default ''  comment '节点类型:tool,skill',
    node_name        varchar(50)  default ''  comment '节点名称',
    ref_id           varchar(50)  default ''  comment '节点执行步骤关联ID',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    sort_no          int   default 0 comment '排序编号',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_workflow_id (workflow_id),
    key idx_node_name (node_name),
    key idx_ref_id (ref_id)
) comment='工作流节点表';


create table if not exists ai_skill
(
    id               int auto_increment  primary key,
    skill_id         varchar(50)  default ''  comment '技能ID',
    skill_name       varchar(50)  default ''  comment '技能名称',
    description      varchar(200) default ''  comment '技能描述',
    system_prompt    varchar(2000) default ''  comment '系统提示词',
    user_prompt      varchar(2000) default ''  comment '用户提示词',
    out_schema       varchar(2000) default ''  comment '输出模式',
    model_name       varchar(50)  default ''  comment '调用模型名称',
    temperature      float  default 0.0 comment '温度参数：0.0-2.0',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_skill_id (skill_id),
    key idx_skill_name (skill_name)
) comment='技能表';


create table if not exists ai_skill_example
(
    id               int auto_increment  primary key,
    skill_id         varchar(50)  default ''  comment '技能ID',
    input_example    varchar(500)  default ''  comment '示例问题',
    output_example   varchar(5000)  default ''  comment '示例答案',
    key idx_skill_id (skill_id),
    key idx_input_example (input_example)
) comment='技能示例表';


create table if not exists ai_tool
(
    id               int auto_increment  primary key,
    tool_id          varchar(50)  default ''  comment '工具ID',
    tool_name        varchar(50)  default ''  comment '工具名称',
    description      varchar(200) default ''  comment '工具描述',
    bean_name        varchar(50)  default ''  comment '工具类全限定名',
    method_name      varchar(50)  default ''  comment '工具方法名',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    sort_no          int   default 0 comment '排序编号',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_tool_id (tool_id),
    key idx_tool_name (tool_name)
) comment='工具表';


create table if not exists ai_agent
(
    id               int auto_increment  primary key,
    agent_id         varchar(50)  default ''  comment '代理ID',
    agent_name       varchar(50)  default ''  comment '代理名称',
    description      varchar(200) default ''  comment '代理描述',
    goal_prompt      varchar(2000) default ''  comment '目标提示词',
    planner_prompt   varchar(2000) default ''  comment '规划提示词',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    sort_no          int   default 0 comment '排序编号',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_agent_id (agent_id),
    key idx_agent_name (agent_name)
) comment='代理表';


create table if not exists ai_model_config
(
    id               int auto_increment  primary key,
    model_name       varchar(50)  default ''  comment '模型名称',
    api_key          varchar(200) default ''  comment 'API密钥',
    base_url          varchar(200) default ''  comment 'API地址',
    temperature      float  default 0.0 comment '温度参数：0.0-2.0',
    max_tokens       int   default 0 comment '最大令牌数',
    top_p            float  default 1.0 comment 'Top P参数：0.0-1.0',
    enable_flag      varchar(10)  default ''  comment '启用标志：是、否',
    create_time      datetime   default current_timestamp comment '创建时间',
    update_time      datetime   default current_timestamp comment '更新时间',
    key idx_model_name (model_name)
) comment='代理示例表';





-- 意图配置
INSERT INTO ai_intent (intent_id, intent_name, description, match_prompt, workflow_id, enable_flag, sort_no)
VALUES
    ('INTENT_CUSTOMER_ANALYSIS', '客户分析', '分析客户风险、等级、消费行为等', '当用户询问客户相关分析、风险评估时匹配', 'WF_CUSTOMER_ANALYSIS', '是', 1),
    ('INTENT_ORDER_QUERY', '订单查询', '查询订单信息、订单状态等', '当用户询问订单、交易记录时匹配', 'WF_ORDER_QUERY', '是', 2);

-- 意图示例
INSERT INTO ai_intent_example (intent_id, question, answer) VALUES
                                                                ('INTENT_CUSTOMER_ANALYSIS', '帮我分析一下这个客户的风险', '好的，我来为您分析客户风险'),
                                                                ('INTENT_CUSTOMER_ANALYSIS', '客户张三的信用等级怎么样', '张三的信用等级是VIP'),
                                                                ('INTENT_ORDER_QUERY', '查一下最近的订单', '好的，为您查询最近订单'),
                                                                ('INTENT_ORDER_QUERY', '这个客户有什么订单记录', '正在查询订单记录');

-- 工作流
INSERT INTO ai_workflow (workflow_id, workflow_name, description, enable_flag, sort_no)
VALUES ('WF_CUSTOMER_ANALYSIS', '客户分析流程', '查询客户信息和订单，进行综合分析', '是', 1);

-- 工作流节点
INSERT INTO ai_workflow_node (workflow_id, node_type, node_name, ref_id, enable_flag, sort_no)
VALUES
    ('WF_CUSTOMER_ANALYSIS', 'tool', '查询客户', 'TOOL_CUSTOMER', '是', 1),
    ('WF_CUSTOMER_ANALYSIS', 'tool', '查询订单', 'TOOL_ORDER', '是', 2),
    ('WF_CUSTOMER_ANALYSIS', 'skill', '客户分析', 'SKILL_CUSTOMER_ANALYSIS', '是', 3);

-- 技能
INSERT INTO ai_skill (skill_id, skill_name, description, system_prompt, user_prompt, model_name, temperature, enable_flag)
VALUES ('SKILL_CUSTOMER_ANALYSIS', '客户风险分析', '综合分析客户信息和订单数据',
        '你是CRM客户分析专家，擅长分析客户风险等级、消费能力和行为偏好。',
        '请根据以下信息分析客户：\n客户信息：${查询客户}\n订单信息：${查询订单}\n请分析客户风险，并给出建议',
        'qwen', 0.7, '是');

-- 工具
INSERT INTO ai_tool (tool_id, tool_name, description, bean_name, method_name, enable_flag, sort_no)
VALUES
    ('TOOL_CUSTOMER', '客户查询工具', '根据客户ID查询客户信息', 'customerTool', 'queryCustomer', '是', 1),
    ('TOOL_ORDER', '订单查询工具', '根据客户ID查询订单列表', 'orderTool', 'queryOrder', '是', 2);

-- 模型配置
INSERT INTO ai_model_config (model_name, api_key, base_url, temperature, max_tokens, top_p, enable_flag)
VALUES ('qwen', 'sk-ws-xxx', 'https://dashscope.aliyuncs.com', 0.7, 2048, 0.9, '是');

```