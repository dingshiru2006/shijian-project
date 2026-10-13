package com.kb.system.module.kb.document.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.kb.document.dto.DocumentDTO;
import com.kb.system.module.kb.document.service.KbDocumentService;
import com.kb.system.module.kb.document.vo.DocumentVO;
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

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/kb/documents")
public class KbDocumentController {

    private final KbDocumentService documentService;

    public KbDocumentController(KbDocumentService documentService) {
        this.documentService = documentService;
    }

    @PreAuthorize("hasAuthority('kb:doc:view')")
    @GetMapping
    public Result<PageResult<DocumentVO>> page(@Valid DocumentDTO.Query query) {
        return Result.ok(documentService.page(query));
    }

    @PreAuthorize("hasAuthority('kb:doc:view')")
    @GetMapping("/{id}")
    public Result<DocumentVO> detail(@PathVariable Long id) {
        return Result.ok(documentService.detail(id));
    }

    @OperationLog(module = "文档管理", type = OperationType.ADD, description = "新增文档")
    @PreAuthorize("hasAuthority('kb:doc:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DocumentDTO.Create dto) {
        return Result.ok(documentService.create(dto));
    }

    @OperationLog(module = "文档管理", type = OperationType.UPDATE, description = "修改文档")
    @PreAuthorize("hasAuthority('kb:doc:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DocumentDTO.Update dto) {
        documentService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "文档管理", type = OperationType.DELETE, description = "删除文档")
    @PreAuthorize("hasAuthority('kb:doc:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok();
    }
}
