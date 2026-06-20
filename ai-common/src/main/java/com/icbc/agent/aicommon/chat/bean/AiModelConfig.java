package com.icbc.agent.aicommon.chat.bean;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("ai_model_config")
public class AiModelConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String modelName;
    private String apiKey;
    private String baseUrl;
    private Float temperature;
    private Integer maxTokens;
    private Float topP;
    private String enableFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

