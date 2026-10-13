package com.kb.system.module.system.log.repository;

import com.kb.system.module.system.log.entity.SysOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 操作日志 Repository（支持组合条件查询）
 */
public interface SysOperationLogRepository extends JpaRepository<SysOperationLog, Long>,
        JpaSpecificationExecutor<SysOperationLog> {
}
