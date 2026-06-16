# AI模型层架构设计

## 架构分层

```
┌─────────────────────────────────────┐
│         ai-web (Controller)         │  ← REST API接口层
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        ai-chat (Service)            │  ← 业务逻辑层
│     调用model层，不直接依赖Spring AI │
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
