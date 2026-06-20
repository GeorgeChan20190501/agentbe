package com.icbc.agent.aicommon.chat.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_skill")
public class AiSkill {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String skillId;
    private String skillName;
    private String description;
    private String systemPrompt;
    private String userPrompt;
    private String outSchema;
    private String modelName;
    private Float temperature;
    private String enableFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
