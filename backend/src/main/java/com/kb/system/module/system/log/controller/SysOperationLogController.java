package com.kb.system.module.system.log.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.system.log.dto.LogQueryDTO;
import com.kb.system.module.system.log.service.SysOperationLogService;
import com.kb.system.module.system.log.vo.OperationLogVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器（AOP 记录，支持组合条件查询）
 */
@RestController
@RequestMapping("/system/logs")
public class SysOperationLogController {

    private final SysOperationLogService operationLogService;

    public SysOperationLogController(SysOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PreAuthorize("hasAuthority('system:log:view')")
    @GetMapping
    public Result<PageResult<OperationLogVO>> page(@Valid LogQueryDTO query) {
        return Result.ok(operationLogService.page(query));
    }

    @PreAuthorize("hasAuthority('system:log:view')")
    @GetMapping("/{id}")
    public Result<OperationLogVO> detail(@PathVariable Long id) {
        return Result.ok(operationLogService.detail(id));
    }

    @OperationLog(module = "操作日志", type = OperationType.DELETE, description = "删除日志")
    @PreAuthorize("hasAuthority('system:log:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        operationLogService.delete(id);
        return Result.ok();
    }
}
