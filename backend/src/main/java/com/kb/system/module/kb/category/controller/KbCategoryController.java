package com.kb.system.module.kb.category.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.kb.category.dto.CategoryDTO;
import com.kb.system.module.kb.category.service.KbCategoryService;
import com.kb.system.module.kb.category.vo.CategoryVO;
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
 * 分类管理控制器
 */
@RestController
@RequestMapping("/kb/categories")
public class KbCategoryController {

    private final KbCategoryService categoryService;

    public KbCategoryController(KbCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PreAuthorize("hasAuthority('kb:category:view')")
    @GetMapping
    public Result<PageResult<CategoryVO>> page(@Valid CategoryDTO.Query query) {
        return Result.ok(categoryService.page(query));
    }

    @PreAuthorize("hasAuthority('kb:category:view')")
    @GetMapping("/all")
    public Result<List<CategoryVO>> all() {
        return Result.ok(categoryService.all());
    }

    @OperationLog(module = "分类管理", type = OperationType.ADD, description = "新增分类")
    @PreAuthorize("hasAuthority('kb:category:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryDTO.Create dto) {
        return Result.ok(categoryService.create(dto));
    }

    @OperationLog(module = "分类管理", type = OperationType.UPDATE, description = "修改分类")
    @PreAuthorize("hasAuthority('kb:category:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO.Update dto) {
        categoryService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "分类管理", type = OperationType.DELETE, description = "删除分类")
    @PreAuthorize("hasAuthority('kb:category:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
