package com.kb.system.module.system.user.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.common.PageQuery;
import com.kb.system.module.system.role.entity.SysRole;
import com.kb.system.module.system.role.repository.SysRoleRepository;
import com.kb.system.module.system.user.dto.UserDTO;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.entity.SysUserRole;
import com.kb.system.module.system.user.repository.SysUserRepository;
import com.kb.system.module.system.user.repository.SysUserRoleRepository;
import com.kb.system.module.system.user.vo.UserVO;
import com.kb.system.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户管理服务：CRUD + 角色分配 + 密码重置
 */
@Service
public class SysUserService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public SysUserService(SysUserRepository userRepository,
                          SysUserRoleRepository userRoleRepository,
                          SysRoleRepository roleRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    public PageResult<UserVO> page(UserDTO.Query query) {
        Specification<SysUser> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getUsername())) {
                predicates.add(cb.like(root.get("username"), "%" + query.getUsername() + "%"));
            }
            if (StringUtils.hasText(query.getNickname())) {
                predicates.add(cb.like(root.get("nickname"), "%" + query.getNickname() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getRoleId() != null) {
                Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
                Root<SysUserRole> userRole = subquery.from(SysUserRole.class);
                subquery.select(userRole.get("userId"))
                        .where(cb.equal(userRole.get("roleId"), query.getRoleId()));
                predicates.add(root.get("id").in(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysUser> page = userRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(page, user -> toVO(user));
    }

    public UserVO detail(Long id) {
        return toVO(getUser(id));
    }

    @Transactional
    public Long create(UserDTO.Create dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        SysUser saved = userRepository.save(user);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRolesInternal(saved.getId(), new LinkedHashSet<>(dto.getRoleIds()));
        }
        return saved.getId();
    }

    @Transactional
    public void update(Long id, UserDTO.Update dto) {
        SysUser user = getUser(id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = getUser(id);
        if (user.getUsername().equals(currentUser.username())) {
            throw new BusinessException("不能删除当前登录用户");
        }
        if (userRoleRepository.findByUserId(id).stream()
                .anyMatch(ur -> isAdminRole(ur.getRoleId()))) {
            throw new BusinessException("管理员账号不允许删除");
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    @Transactional
    public void assignRoles(Long id, UserDTO.AssignRoles dto) {
        getUser(id);
        assignRolesInternal(id, new LinkedHashSet<>(dto.getRoleIds()));
    }

    @Transactional
    public void resetPassword(Long id, UserDTO.ResetPassword dto) {
        SysUser user = getUser(id);
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    /** 全部用户（下拉选择用） */
    public List<UserVO> all() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toVO).toList();
    }

    // ---------------- 内部方法 ----------------

    private void assignRolesInternal(Long userId, Set<Long> roleIds) {
        List<SysRole> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("包含不存在的角色");
        }
        userRoleRepository.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }

    private boolean isAdminRole(Long roleId) {
        return roleRepository.findById(roleId)
                .map(role -> "ADMIN".equals(role.getRoleCode())).orElse(false);
    }

    private SysUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private UserVO toVO(SysUser user) {
        List<SysUserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<String> roleNames = List.of();
        if (!roleIds.isEmpty()) {
            roleNames = roleRepository.findAllById(roleIds).stream()
                    .map(SysRole::getRoleName).toList();
        }
        return UserVO.from(user, roleIds, roleNames);
    }
}
