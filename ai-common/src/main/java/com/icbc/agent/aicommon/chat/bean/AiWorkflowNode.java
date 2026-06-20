package com.icbc.agent.aicommon.chat.bean;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("ai_workflow_node")
public class AiWorkflowNode {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String workflowId;
    private String nodeType;  // "tool" or "skill"
    private String nodeName;
    private String refId;
    private String nodeConfig; // 新增：JSON配置
    private String enableFlag;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
