package com.kb.system.security;

import com.kb.system.common.BusinessException;
import com.kb.system.module.system.permission.entity.SysPermission;
import com.kb.system.module.system.permission.repository.SysPermissionRepository;
import com.kb.system.module.system.role.entity.SysRole;
import com.kb.system.module.system.role.repository.SysRoleRepository;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.repository.SysUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户详情加载：用户名 -> 用户 + 角色编码 + 权限编码
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;

    public UserDetailsServiceImpl(SysUserRepository userRepository,
                                  SysRoleRepository roleRepository,
                                  SysPermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        Set<String> roles = roleRepository.findRoleCodesByUserId(user.getId());
        Set<String> permissions = permissionRepository.findPermCodesByUserId(user.getId());
        return new LoginUser(user, roles, permissions);
    }
}
