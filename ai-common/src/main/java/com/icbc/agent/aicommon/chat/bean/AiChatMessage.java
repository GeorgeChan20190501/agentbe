package com.icbc.agent.aicommon.chat.bean;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String sessionId;
    private String role;
    private String content;
    private String contentHtml;
    private String intentId;
    private String workflowId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
