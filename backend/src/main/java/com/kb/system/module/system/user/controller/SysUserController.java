package com.kb.system.module.system.user.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.system.user.dto.UserDTO;
import com.kb.system.module.system.user.service.SysUserService;
import com.kb.system.module.system.user.vo.UserVO;
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
 * 用户管理控制器
 */
@RestController
@RequestMapping("/system/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping
    public Result<PageResult<UserVO>> page(@Valid UserDTO.Query query) {
        return Result.ok(userService.page(query));
    }

    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/all")
    public Result<java.util.List<UserVO>> all() {
        return Result.ok(userService.all());
    }

    @PreAuthorize("hasAuthority('system:user:view')")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @OperationLog(module = "用户管理", type = OperationType.ADD, description = "新增用户")
    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserDTO.Create dto) {
        return Result.ok(userService.create(dto));
    }

    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "修改用户")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO.Update dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "用户管理", type = OperationType.DELETE, description = "删除用户")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @OperationLog(module = "用户管理", type = OperationType.ASSIGN, description = "分配角色")
    @PreAuthorize("hasAuthority('system:user:assign-role')")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserDTO.AssignRoles dto) {
        userService.assignRoles(id, dto);
        return Result.ok();
    }

    @OperationLog(module = "用户管理", type = OperationType.PASSWORD, description = "重置密码")
    @PreAuthorize("hasAuthority('system:user:reset-pwd')")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody UserDTO.ResetPassword dto) {
        userService.resetPassword(id, dto);
        return Result.ok();
    }
}
