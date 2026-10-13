package com.kb.system.module.system.permission.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统功能权限表（菜单权限 + 按钮/操作权限）
 */
@Entity
@Table(name = "sys_permission")
@Getter
@Setter
public class SysPermission extends BaseEntity {

    /** 权限编码，如 kb:doc:add */
    @Column(name = "perm_code", nullable = false, unique = true, length = 100)
    private String permCode;

    /** 权限名称 */
    @Column(name = "perm_name", nullable = false, length = 50)
    private String permName;

    /** 所属模块，如 系统管理 / 知识库管理 */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** 类型：1-菜单，2-按钮 */
    @Column(name = "type", nullable = false)
    private Integer type;

    /** 前端路由路径（菜单权限使用） */
    @Column(name = "route_path", length = 100)
    private String routePath;
}
