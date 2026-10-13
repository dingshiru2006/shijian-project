package com.kb.system.module.system.log.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统操作日志表（AOP 记录重要写操作）
 */
@Entity
@Table(name = "sys_operation_log")
@Getter
@Setter
public class SysOperationLog extends BaseEntity {

    /** 功能模块 */
    @Column(name = "module", length = 50)
    private String module;

    /** 操作类型：ADD/UPDATE/DELETE/LOGIN/LOGOUT/ASSIGN/REBUILD 等 */
    @Column(name = "operation_type", length = 20)
    private String operationType;

    /** 操作描述 */
    @Column(name = "description", length = 255)
    private String description;

    /** 请求方法全限定名 */
    @Column(name = "method", length = 200)
    private String method;

    /** 请求 URI */
    @Column(name = "request_uri", length = 255)
    private String requestUri;

    /** 请求方式 */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /** 请求参数（JSON，超长截断） */
    @Column(name = "request_params", columnDefinition = "text")
    private String requestParams;

    /** 操作人账号 */
    @Column(name = "operator", length = 50)
    private String operator;

    /** 操作人 ID */
    @Column(name = "operator_id")
    private Long operatorId;

    /** 操作人 IP */
    @Column(name = "operator_ip", length = 50)
    private String operatorIp;

    /** 操作结果：1-成功，0-失败 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 异常信息 */
    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    /** 耗时（毫秒） */
    @Column(name = "cost_time")
    private Long costTime;
}
