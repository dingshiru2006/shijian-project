package com.kb.system.module.system.role.vo;

import com.kb.system.module.system.role.entity.SysRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象
 */
@Data
@AllArgsConstructor
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    /** 已分配权限 ID 列表（编辑回显用） */
    private List<Long> permissionIds;

    public static RoleVO from(SysRole role, List<Long> permissionIds) {
        return new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(),
                role.getDescription(), role.getStatus(), role.getSort(),
                role.getCreatedAt(), permissionIds);
    }
}
