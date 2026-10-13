package com.kb.system.module.kb.tag.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.kb.tag.dto.TagDTO;
import com.kb.system.module.kb.tag.service.KbTagService;
import com.kb.system.module.kb.tag.vo.TagVO;
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
 * 标签管理控制器
 */
@RestController
@RequestMapping("/kb/tags")
public class KbTagController {

    private final KbTagService tagService;

    public KbTagController(KbTagService tagService) {
        this.tagService = tagService;
    }

    @PreAuthorize("hasAuthority('kb:tag:view')")
    @GetMapping
    public Result<PageResult<TagVO>> page(@Valid TagDTO.Query query) {
        return Result.ok(tagService.page(query));
    }

    @PreAuthorize("hasAuthority('kb:tag:view')")
    @GetMapping("/all")
    public Result<List<TagVO>> all() {
        return Result.ok(tagService.all());
    }

    @OperationLog(module = "标签管理", type = OperationType.ADD, description = "新增标签")
    @PreAuthorize("hasAuthority('kb:tag:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagDTO.Create dto) {
        return Result.ok(tagService.create(dto));
    }

    @OperationLog(module = "标签管理", type = OperationType.UPDATE, description = "修改标签")
    @PreAuthorize("hasAuthority('kb:tag:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO.Update dto) {
        tagService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "标签管理", type = OperationType.DELETE, description = "删除标签")
    @PreAuthorize("hasAuthority('kb:tag:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
