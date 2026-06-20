package com.icbc.agent.aicommon.chat.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_intent_example")
public class AiIntentExample {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String intentId;
    private String question;
    private String answer;
}
