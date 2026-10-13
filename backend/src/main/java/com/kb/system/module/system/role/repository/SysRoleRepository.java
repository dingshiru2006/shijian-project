package com.kb.system.module.system.role.repository;

import com.kb.system.module.system.role.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

/**
 * 角色 Repository
 */
public interface SysRoleRepository extends JpaRepository<SysRole, Long>,
        JpaSpecificationExecutor<SysRole> {

    Optional<SysRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleCodeAndIdNot(String roleCode, Long id);

    /** 查询用户拥有的角色编码 */
    @Query("select distinct r.roleCode from SysRole r, SysUserRole ur " +
           "where ur.roleId = r.id and ur.userId = :userId")
    Set<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
