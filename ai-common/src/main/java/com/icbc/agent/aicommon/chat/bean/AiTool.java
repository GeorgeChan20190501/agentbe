package com.icbc.agent.aicommon.chat.bean;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_tool")
public class AiTool {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String toolId;
    private String toolName;
    private String description;
    private String beanName;
    private String methodName;
    private String enableFlag;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
