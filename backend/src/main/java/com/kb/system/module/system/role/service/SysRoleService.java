package com.kb.system.module.system.role.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.system.permission.entity.SysPermission;
import com.kb.system.module.system.permission.entity.SysRolePermission;
import com.kb.system.module.system.permission.repository.SysPermissionRepository;
import com.kb.system.module.system.permission.repository.SysRolePermissionRepository;
import com.kb.system.module.system.role.dto.RoleDTO;
import com.kb.system.module.system.role.entity.SysRole;
import com.kb.system.module.system.role.repository.SysRoleRepository;
import com.kb.system.module.system.role.vo.RoleVO;
import com.kb.system.module.system.user.repository.SysUserRoleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 角色管理服务：CRUD + 权限分配
 */
@Service
public class SysRoleService {

    private final SysRoleRepository roleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysUserRoleRepository userRoleRepository;

    public SysRoleService(SysRoleRepository roleRepository,
                          SysRolePermissionRepository rolePermissionRepository,
                          SysPermissionRepository permissionRepository,
                          SysUserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public PageResult<RoleVO> page(RoleDTO.Query query) {
        Specification<SysRole> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getRoleCode())) {
                predicates.add(cb.like(root.get("roleCode"), "%" + query.getRoleCode() + "%"));
            }
            if (StringUtils.hasText(query.getRoleName())) {
                predicates.add(cb.like(root.get("roleName"), "%" + query.getRoleName() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysRole> page = roleRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "sort")));
        return PageResult.from(page, role -> toVO(role));
    }

    /** 全部角色（分配用） */
    public List<RoleVO> all() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "sort")).stream()
                .map(this::toVO).toList();
    }

    public RoleVO detail(Long id) {
        return toVO(getRole(id));
    }

    @Transactional
    public Long create(RoleDTO.Create dto) {
        if (roleRepository.existsByRoleCode(dto.getRoleCode())) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        role.setSort(dto.getSort());
        return roleRepository.save(role).getId();
    }

    @Transactional
    public void update(Long id, RoleDTO.Update dto) {
        SysRole role = getRole(id);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        role.setSort(dto.getSort());
        roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = getRole(id);
        long usedByUsers = userRoleRepository.countByRoleId(id);
        if (usedByUsers > 0) {
            throw new BusinessException("该角色已分配给 " + usedByUsers + " 个用户，无法删除");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    @Transactional
    public void assignPermissions(Long id, RoleDTO.AssignPermissions dto) {
        getRole(id);
        Set<Long> permissionIds = dto.getPermissionIds() == null ? Set.of()
                : new java.util.LinkedHashSet<>(dto.getPermissionIds());
        List<SysPermission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BusinessException("包含不存在的权限");
        }
        rolePermissionRepository.deleteByRoleId(id);
        for (Long permissionId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.save(rp);
        }
    }

    private SysRole getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
    }

    private RoleVO toVO(SysRole role) {
        List<Long> permissionIds = rolePermissionRepository.findByRoleId(role.getId())
                .stream().map(SysRolePermission::getPermissionId).toList();
        return RoleVO.from(role, permissionIds);
    }
}
