package com.icbc.agent.aicommon.chat.bean;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("ai_chat_session")
public class AiChatSession {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String sessionId;
    private String userId;
    private String sessionName;
    private String lastMessage;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

