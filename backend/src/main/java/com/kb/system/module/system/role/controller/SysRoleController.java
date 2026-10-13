package com.kb.system.module.system.role.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.system.role.dto.RoleDTO;
import com.kb.system.module.system.role.service.SysRoleService;
import com.kb.system.module.system.role.vo.RoleVO;
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
 * 角色管理控制器
 */
@RestController
@RequestMapping("/system/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAuthority('system:role:view')")
    @GetMapping
    public Result<PageResult<RoleVO>> page(@Valid RoleDTO.Query query) {
        return Result.ok(roleService.page(query));
    }

    @PreAuthorize("hasAuthority('system:role:view')")
    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.all());
    }

    @PreAuthorize("hasAuthority('system:role:view')")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @OperationLog(module = "角色管理", type = OperationType.ADD, description = "新增角色")
    @PreAuthorize("hasAuthority('system:role:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleDTO.Create dto) {
        return Result.ok(roleService.create(dto));
    }

    @OperationLog(module = "角色管理", type = OperationType.UPDATE, description = "修改角色")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO.Update dto) {
        roleService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "角色管理", type = OperationType.DELETE, description = "删除角色")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @OperationLog(module = "角色管理", type = OperationType.ASSIGN, description = "分配权限")
    @PreAuthorize("hasAuthority('system:role:assign-perm')")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @Valid @RequestBody RoleDTO.AssignPermissions dto) {
        roleService.assignPermissions(id, dto);
        return Result.ok();
    }
}
