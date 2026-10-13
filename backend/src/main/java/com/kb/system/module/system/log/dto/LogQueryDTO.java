package com.kb.system.module.system.log.dto;

import com.kb.system.common.PageQuery;
import lombok.Data;

/**
 * 操作日志查询条件（支持模块、类型、操作人、结果、时间范围组合筛选）
 */
@Data
public class LogQueryDTO extends PageQuery {

    /** 功能模块（模糊） */
    private String module;

    /** 操作类型 */
    private String operationType;

    /** 操作人（模糊） */
    private String operator;

    /** 操作结果：1-成功，0-失败 */
    private Integer status;

    /** 开始时间 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 结束时间 yyyy-MM-dd HH:mm:ss */
    private String endTime;
}
