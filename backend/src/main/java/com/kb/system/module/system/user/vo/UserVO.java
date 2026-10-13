package com.kb.system.module.system.user.vo;

import com.kb.system.module.system.user.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象
 */
@Data
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    /** 已分配角色 ID 列表 */
    private List<Long> roleIds;
    /** 已分配角色名称列表 */
    private List<String> roleNames;

    public static UserVO from(SysUser user, List<Long> roleIds, List<String> roleNames) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getEmail(), user.getPhone(), user.getAvatar(), user.getStatus(),
                user.getLastLoginAt(), user.getLastLoginIp(), user.getCreatedAt(),
                roleIds, roleNames);
    }
}
