package com.kb.system.module.system.log.service;

import com.kb.system.common.PageResult;
import com.kb.system.module.system.log.dto.LogQueryDTO;
import com.kb.system.module.system.log.entity.SysOperationLog;
import com.kb.system.module.system.log.repository.SysOperationLogRepository;
import com.kb.system.module.system.log.vo.OperationLogVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志服务：AOP 异步落库 + 组合条件分页查询
 */
@Service
public class SysOperationLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperationLogRepository logRepository;

    public SysOperationLogService(SysOperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * 异步保存操作日志（由 AOP 切面调用）
     */
    @Async("taskExecutor")
    public void saveAsync(SysOperationLog log) {
        try {
            logRepository.save(log);
        } catch (Exception e) {
            // 日志写入失败不影响主业务，仅记录警告
            org.slf4j.LoggerFactory.getLogger(SysOperationLogService.class)
                    .warn("操作日志异步写入失败: {}", e.getMessage());
        }
    }

    public PageResult<OperationLogVO> page(LogQueryDTO query) {
        Specification<SysOperationLog> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getModule())) {
                predicates.add(cb.like(root.get("module"), "%" + query.getModule() + "%"));
            }
            if (StringUtils.hasText(query.getOperationType())) {
                predicates.add(cb.equal(root.get("operationType"), query.getOperationType()));
            }
            if (StringUtils.hasText(query.getOperator())) {
                predicates.add(cb.like(root.get("operator"), "%" + query.getOperator() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (StringUtils.hasText(query.getStartTime())) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        LocalDateTime.parse(query.getStartTime(), DATE_TIME_FORMATTER)));
            }
            if (StringUtils.hasText(query.getEndTime())) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        LocalDateTime.parse(query.getEndTime(), DATE_TIME_FORMATTER)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysOperationLog> page = logRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(page, OperationLogVO::from);
    }

    public OperationLogVO detail(Long id) {
        SysOperationLog log = logRepository.findById(id)
                .orElseThrow(() -> new com.kb.system.common.BusinessException("日志不存在"));
        return OperationLogVO.from(log);
    }

    @Transactional
    public void delete(Long id) {
        SysOperationLog log = logRepository.findById(id)
                .orElseThrow(() -> new com.kb.system.common.BusinessException("日志不存在"));
        logRepository.delete(log);
    }
}
