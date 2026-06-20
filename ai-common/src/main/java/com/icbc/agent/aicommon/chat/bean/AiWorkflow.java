package com.icbc.agent.aicommon.chat.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_workflow")
public class AiWorkflow {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String workflowId;
    private String workflowName;
    private String description;
    private String workflowJson;
    private String enableFlag;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

