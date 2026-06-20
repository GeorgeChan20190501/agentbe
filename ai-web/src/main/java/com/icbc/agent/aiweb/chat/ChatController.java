package com.icbc.agent.aiweb.chat;

import com.icbc.agent.aichat.service.ChatService;
import com.icbc.agent.aiweb.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ai/message")
    public Result<String> chat(@RequestBody ChatRequest request) {
        try {
            String response = chatService.chat(request.getMessage());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI对话失败: " + e.getMessage());
        }
    }

    /**
     * 流式对话接口
     */
    @PostMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Object> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request.getMessage());
    }

    @PostMapping("/auth/login")
    public String login() {
        return "1";
    }

    /**
     * 聊天请求DTO
     */
    @lombok.Data
    public static class ChatRequest {
        private String message;
    }
}
