package com.kb.system.module.system.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 登录成功响应
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT Token */
    private String token;

    /** Token 类型 */
    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    /** 当前用户信息（含角色与权限） */
    private UserInfoVO userInfo;

    /**
     * 用户信息（角色 + 权限，前端用于菜单与按钮级控制）
     */
    @Data
    @AllArgsConstructor
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        /** 角色编码列表 */
        private List<String> roles;
        /** 权限编码列表 */
        private List<String> permissions;
    }
}
