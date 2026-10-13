package com.kb.system.module.search.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.search.service.IndexRebuildService;
import com.kb.system.module.search.vo.RebuildTaskVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ES 索引重建控制器：提交任务（异步）/ 任务列表 / 任务详情
 */
@RestController
@RequestMapping("/rebuild")
public class IndexRebuildController {

    private final IndexRebuildService rebuildService;

    public IndexRebuildController(IndexRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    /**
     * 提交全量重建任务：写入任务表 + 发送 RabbitMQ 消息，后台消费执行
     */
    @OperationLog(module = "索引管理", type = OperationType.REBUILD, description = "提交索引全量重建任务")
    @PreAuthorize("hasAuthority('kb:rebuild')")
    @PostMapping("/tasks")
    public Result<RebuildTaskVO> submit() {
        return Result.ok(rebuildService.submitTask());
    }

    @GetMapping("/tasks")
    public Result<PageResult<RebuildTaskVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(rebuildService.page(pageNum, pageSize));
    }

    @GetMapping("/tasks/{id}")
    public Result<RebuildTaskVO> detail(@PathVariable Long id) {
        return Result.ok(rebuildService.detail(id));
    }
}
