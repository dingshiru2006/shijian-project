package com.kb.system.module.kb.knowledgebase.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.kb.knowledgebase.dto.KbDTO;
import com.kb.system.module.kb.knowledgebase.service.KbKnowledgeBaseService;
import com.kb.system.module.kb.knowledgebase.vo.KbVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/kb/knowledge-bases")
public class KbKnowledgeBaseController {

    private final KbKnowledgeBaseService knowledgeBaseService;

    public KbKnowledgeBaseController(KbKnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PreAuthorize("hasAuthority('kb:kb:view')")
    @GetMapping
    public Result<PageResult<KbVO>> page(@Valid KbDTO.Query query) {
        return Result.ok(knowledgeBaseService.page(query));
    }

    @PreAuthorize("hasAuthority('kb:kb:view')")
    @GetMapping("/all")
    public Result<List<KbVO>> all() {
        return Result.ok(knowledgeBaseService.all());
    }

    @PreAuthorize("hasAuthority('kb:kb:view')")
    @GetMapping("/{id}")
    public Result<KbVO> detail(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.detail(id));
    }

    @OperationLog(module = "知识库管理", type = OperationType.ADD, description = "新增知识库")
    @PreAuthorize("hasAuthority('kb:kb:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody KbDTO.Create dto) {
        return Result.ok(knowledgeBaseService.create(dto));
    }

    @OperationLog(module = "知识库管理", type = OperationType.UPDATE, description = "修改知识库")
    @PreAuthorize("hasAuthority('kb:kb:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody KbDTO.Update dto) {
        knowledgeBaseService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "知识库管理", type = OperationType.DELETE, description = "删除知识库")
    @PreAuthorize("hasAuthority('kb:kb:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.ok();
    }
}
