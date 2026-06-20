package com.icbc.agent.aicommon.chat.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_intent")
public class AiIntent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String intentId;
    private String intentName;
    private String description;
    private String matchPrompt;
    private String workflowId;
    private String enableFlag;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

