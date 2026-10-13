package com.kb.system.module.system.permission.vo;

import com.kb.system.module.system.permission.entity.SysPermission;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限视图对象
 */
@Data
@AllArgsConstructor
public class PermissionVO {

    private Long id;
    private String permCode;
    private String permName;
    private String module;
    private Integer type;
    private String routePath;
    private LocalDateTime createdAt;

    public static PermissionVO from(SysPermission permission) {
        return new PermissionVO(permission.getId(), permission.getPermCode(),
                permission.getPermName(), permission.getModule(), permission.getType(),
                permission.getRoutePath(), permission.getCreatedAt());
    }
}
