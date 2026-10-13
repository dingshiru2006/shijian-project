package com.kb.system.module.system.permission.repository;

import com.kb.system.module.system.permission.entity.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * 角色-权限关联 Repository
 */
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, Long> {

    List<SysRolePermission> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    List<SysRolePermission> findByRoleIdIn(Collection<Long> roleIds);
}
