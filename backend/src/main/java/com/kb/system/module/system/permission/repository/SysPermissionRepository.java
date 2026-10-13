package com.kb.system.module.system.permission.repository;

import com.kb.system.module.system.permission.entity.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * 权限 Repository
 */
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long>,
        JpaSpecificationExecutor<SysPermission> {

    boolean existsByPermCode(String permCode);

    boolean existsByPermCodeAndIdNot(String permCode, Long id);

    /** 查询用户通过角色获得的全部权限编码 */
    @Query("select distinct p.permCode from SysPermission p, SysRolePermission rp, SysUserRole ur " +
           "where rp.permissionId = p.id and ur.roleId = rp.roleId and ur.userId = :userId")
    Set<String> findPermCodesByUserId(@Param("userId") Long userId);

    /** 查询全部模块名（权限分组用） */
    @Query("select distinct p.module from SysPermission p order by p.module")
    List<String> findDistinctModules();
}
