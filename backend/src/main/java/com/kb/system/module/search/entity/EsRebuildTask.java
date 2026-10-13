package com.kb.system.module.search.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Elasticsearch 索引全量重建任务表（异步执行，RabbitMQ 消费）
 */
@Entity
@Table(name = "es_rebuild_task")
@Getter
@Setter
public class EsRebuildTask extends BaseEntity {

    /** 任务编号（唯一） */
    @Column(name = "task_no", nullable = false, unique = true, length = 50)
    private String taskNo;

    /** 触发方式：1-手动触发 */
    @Column(name = "trigger_type", nullable = false)
    private Integer triggerType;

    /** 状态：0-待执行，1-进行中，2-成功，3-失败 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 待索引文档总数 */
    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    /** 索引成功数量 */
    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    /** 索引失败数量 */
    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    /** 任务信息（失败原因等） */
    @Column(name = "message", length = 500)
    private String message;

    /** 开始执行时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 执行完成时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 任务创建人 ID */
    @Column(name = "created_by")
    private Long createdBy;
}
