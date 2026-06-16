# Spring AI 千问大模型集成说明

> **架构说明**: 本项目采用分层架构，ai-model层封装模型抽象，ai-chat层调用model层。
> 详见 [MODEL_LAYER_ARCHITECTURE.md](MODEL_LAYER_ARCHITECTURE.md)

## 已完成的工作

### 1. 依赖配置
- ✅ 在父pom.xml中添加Spring AI BOM依赖管理
- ✅ 在ai-chat模块添加spring-ai-starter-model-dashscope依赖

### 2. 后端服务
- ✅ 创建ChatService服务类，封装千问调用逻辑
- ✅ 修改ChatController接收消息参数并返回AI回复
- ✅ 统一API路径为 `/api/ai/message`

### 3. 前端适配
- ✅ 更新message.ts接口调用，发送JSON格式数据
- ✅ 调整响应数据类型为string

### 4. 配置文件
- ✅ 在application.properties中添加千问配置

## 使用前配置

### 获取API Key
1. 访问阿里云DashScope平台: https://dashscope.console.aliyun.com/
2. 注册/登录账号
3. 创建API Key

### 配置API Key
在 `agentbe/ai-start/src/main/resources/application.properties` 中：

```properties
# 方式1: 直接配置（不推荐）
spring.ai.dashscope.api-key=sk-your-api-key-here

# 方式2: 环境变量（推荐）
spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}
```

**推荐使用环境变量方式：**
```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-your-api-key-here"

# Linux/Mac
export DASHSCOPE_API_KEY="sk-your-api-key-here"
```

### 选择模型
目前配置的是 `qwen-turbo`，可选模型：
- `qwen-turbo` - 快速响应，适合日常对话
- `qwen-plus` - 平衡性能和成本
- `qwen-max` - 最强性能，适合复杂任务

修改配置：
```properties
spring.ai.dashscope.chat.options.model=qwen-plus
```

## API接口说明

### 聊天接口
- **URL**: `POST /api/ai/message`
- **请求体**:
```json
{
  "message": "你好，请介绍一下你自己"
}
```
- **响应**:
```json
{
  "code": 0,
  "msg": "success",
  "data": "你好！我是工晓数，你的智能助手..."
}
```

## 测试步骤

1. 启动后端服务
```bash
cd agentbe
mvn clean install
cd ai-start
mvn spring-boot:run
```

2. 启动前端服务
```bash
cd agentfe
npm install
npm run dev
```

3. 在浏览器中打开前端页面，输入消息测试

## 常见问题

### 1. API Key无效
检查是否已正确配置API Key，确保没有多余空格

### 2. 网络超时
DashScope服务需要外网访问，确保服务器可以访问阿里云API

### 3. 依赖下载失败
检查Maven仓库配置，可能需要配置阿里云Maven镜像

## 扩展功能

### 添加系统提示词
```java
public String chat(String message) {
    return chatClient.prompt()
            .system("你是一个专业的数据助手，名叫工晓数")
            .user(message)
            .call()
            .content();
}
```

### 流式响应
```java
public Flux<String> streamChat(String message) {
    return chatClient.prompt()
            .user(message)
            .stream()
            .content();
}
```

### 多轮对话上下文
```java
public String chatWithHistory(String message, List<Message> history) {
    return chatClient.prompt()
            .messages(history)
            .user(message)
            .call()
            .content();
}
```
