package com.icbc.agent.aiweb.chat;

import com.icbc.agent.aicommon.chat.Chat;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ComponentScan("com.icbc.agent.*")
public class ChatController {

    @GetMapping("/chat")
    public String chat() {
        return new Chat().getCommon();
    }
}
