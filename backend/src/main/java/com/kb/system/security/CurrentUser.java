package com.kb.system.security;

import com.kb.system.common.BusinessException;
import com.kb.system.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户工具（基于安全上下文）
 */
@Component
public class CurrentUser {

    public LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public Long id() {
        return get().getId();
    }

    public String username() {
        return get().getUsername();
    }
}
