package com.kb.system.module.system.log.vo;

import com.kb.system.module.system.log.entity.SysOperationLog;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象
 */
@Data
@AllArgsConstructor
public class OperationLogVO {

    private Long id;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String requestUri;
    private String requestMethod;
    private String requestParams;
    private String operator;
    private Long operatorId;
    private String operatorIp;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime createdAt;

    public static OperationLogVO from(SysOperationLog log) {
        return new OperationLogVO(log.getId(), log.getModule(), log.getOperationType(),
                log.getDescription(), log.getMethod(), log.getRequestUri(), log.getRequestMethod(),
                log.getRequestParams(), log.getOperator(), log.getOperatorId(), log.getOperatorIp(),
                log.getStatus(), log.getErrorMsg(), log.getCostTime(), log.getCreatedAt());
    }
}
