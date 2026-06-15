package com.icbc.agent.aiweb.chat;

import com.icbc.agent.aicommon.chat.Chat;
import com.icbc.agent.aiweb.common.Result;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ComponentScan("com.icbc.agent.*")
public class ChatController {

    @PostMapping("ai/message")
    public Result<String> chat() {
        return Result.success(new Chat().getCommon());
    }


    @PostMapping("/auth/login")
    public String login() {
        return "1";
    }
}
