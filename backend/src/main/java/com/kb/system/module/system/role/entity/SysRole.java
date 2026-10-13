package com.kb.system.module.system.role.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统角色表
 */
@Entity
@Table(name = "sys_role")
@Getter
@Setter
public class SysRole extends BaseEntity {

    /** 角色编码，如 ADMIN / EDITOR / VIEWER */
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    /** 角色名称 */
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    /** 角色描述 */
    @Column(name = "description", length = 255)
    private String description;

    /** 状态：1-启用，0-禁用 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 显示排序 */
    @Column(name = "sort", nullable = false)
    private Integer sort;
}
