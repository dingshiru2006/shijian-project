package com.kb.system.module.search.vo;

import com.kb.system.module.search.entity.EsRebuildTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 索引重建任务视图对象
 */
@Data
@AllArgsConstructor
public class RebuildTaskVO {

    private Long id;
    private String taskNo;
    private Integer triggerType;
    private Integer status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public static RebuildTaskVO from(EsRebuildTask task) {
        return new RebuildTaskVO(task.getId(), task.getTaskNo(), task.getTriggerType(),
                task.getStatus(), task.getTotalCount(), task.getSuccessCount(),
                task.getFailCount(), task.getMessage(), task.getStartedAt(),
                task.getFinishedAt(), task.getCreatedAt());
    }
}
