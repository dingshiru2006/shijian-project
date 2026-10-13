package com.kb.system.module.system.user.repository;

import com.kb.system.module.system.user.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户-角色关联 Repository
 */
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    List<SysUserRole> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /** 统计角色被多少用户使用 */
    long countByRoleId(Long roleId);
}
