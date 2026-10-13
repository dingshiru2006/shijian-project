package com.kb.system.module.system.auth.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.Result;
import com.kb.system.module.system.auth.dto.LoginDTO;
import com.kb.system.module.system.auth.dto.PasswordUpdateDTO;
import com.kb.system.module.system.auth.service.AuthService;
import com.kb.system.module.system.auth.vo.LoginVO;
import com.kb.system.security.CurrentUser;
import com.kb.system.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证控制器：登录 / 当前用户 / 退出 / 修改密码
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    /**
     * 登录：BCrypt 校验 -> JWT + Redis 登录态
     */
    @OperationLog(module = "认证中心", type = OperationType.LOGIN, description = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        return Result.ok(authService.login(dto, clientIp));
    }

    /**
     * 当前登录用户信息（角色 + 权限）
     */
    @GetMapping("/info")
    public Result<LoginVO.UserInfoVO> info() {
        LoginUser loginUser = currentUser.get();
        return Result.ok(new LoginVO.UserInfoVO(loginUser.getId(), loginUser.getUsername(),
                loginUser.getNickname(), loginUser.getAvatar(),
                new ArrayList<>(loginUser.getRoles()), new ArrayList<>(loginUser.getPermissions())));
    }

    /**
     * 退出登录：删除 Redis 登录态，Token 立即失效
     */
    @OperationLog(module = "认证中心", type = OperationType.LOGOUT, description = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(currentUser.id());
        return Result.ok();
    }

    /**
     * 修改密码（修改后需重新登录）
     */
    @OperationLog(module = "认证中心", type = OperationType.PASSWORD, description = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        authService.updatePassword(currentUser.id(), dto);
        return Result.ok();
    }
}
