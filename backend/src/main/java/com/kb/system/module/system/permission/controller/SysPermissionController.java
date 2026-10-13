package com.kb.system.module.system.permission.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.system.permission.dto.PermissionDTO;
import com.kb.system.module.system.permission.service.SysPermissionService;
import com.kb.system.module.system.permission.vo.PermissionVO;
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
 * 权限管理控制器
 */
@RestController
@RequestMapping("/system/permissions")
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PreAuthorize("hasAuthority('system:perm:view')")
    @GetMapping
    public Result<PageResult<PermissionVO>> page(@Valid PermissionDTO.Query query) {
        return Result.ok(permissionService.page(query));
    }

    @PreAuthorize("hasAuthority('system:perm:view')")
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.ok(permissionService.modules());
    }

    @OperationLog(module = "权限管理", type = OperationType.ADD, description = "新增权限")
    @PreAuthorize("hasAuthority('system:perm:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PermissionDTO.Create dto) {
        return Result.ok(permissionService.create(dto));
    }

    @OperationLog(module = "权限管理", type = OperationType.UPDATE, description = "修改权限")
    @PreAuthorize("hasAuthority('system:perm:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionDTO.Update dto) {
        permissionService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "权限管理", type = OperationType.DELETE, description = "删除权限")
    @PreAuthorize("hasAuthority('system:perm:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return Result.ok();
    }
}
