package com.kb.system.module.system.permission.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.system.permission.dto.PermissionDTO;
import com.kb.system.module.system.permission.entity.SysPermission;
import com.kb.system.module.system.permission.repository.SysPermissionRepository;
import com.kb.system.module.system.permission.repository.SysRolePermissionRepository;
import com.kb.system.module.system.permission.vo.PermissionVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理服务：CRUD + 模块列表
 */
@Service
public class SysPermissionService {

    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;

    public SysPermissionService(SysPermissionRepository permissionRepository,
                                SysRolePermissionRepository rolePermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public PageResult<PermissionVO> page(PermissionDTO.Query query) {
        Specification<SysPermission> specification = (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getModule())) {
                predicates.add(cb.like(root.get("module"), "%" + query.getModule() + "%"));
            }
            if (StringUtils.hasText(query.getKeyword())) {
                predicates.add(cb.or(
                        cb.like(root.get("permCode"), "%" + query.getKeyword() + "%"),
                        cb.like(root.get("permName"), "%" + query.getKeyword() + "%")));
            }
            if (query.getType() != null) {
                predicates.add(cb.equal(root.get("type"), query.getType()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<SysPermission> page = permissionRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "module").and(Sort.by(Sort.Direction.ASC, "id"))));
        return PageResult.from(page, PermissionVO::from);
    }

    public List<String> modules() {
        return permissionRepository.findDistinctModules();
    }

    @Transactional
    public Long create(PermissionDTO.Create dto) {
        if (permissionRepository.existsByPermCode(dto.getPermCode())) {
            throw new BusinessException("权限编码已存在");
        }
        SysPermission permission = new SysPermission();
        permission.setPermCode(dto.getPermCode());
        permission.setPermName(dto.getPermName());
        permission.setModule(dto.getModule());
        permission.setType(dto.getType());
        permission.setRoutePath(dto.getRoutePath());
        return permissionRepository.save(permission).getId();
    }

    @Transactional
    public void update(Long id, PermissionDTO.Update dto) {
        SysPermission permission = getPermission(id);
        permission.setPermName(dto.getPermName());
        permission.setModule(dto.getModule());
        permission.setType(dto.getType());
        permission.setRoutePath(dto.getRoutePath());
        permissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        SysPermission permission = getPermission(id);
        long referenced = rolePermissionRepository.findAll().stream()
                .filter(rp -> rp.getPermissionId().equals(id)).count();
        if (referenced > 0) {
            throw new BusinessException("该权限已分配给角色，无法删除");
        }
        permissionRepository.delete(permission);
    }

    private SysPermission getPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("权限不存在"));
    }
}
