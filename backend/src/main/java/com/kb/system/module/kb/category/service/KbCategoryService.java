package com.kb.system.module.kb.category.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.kb.category.dto.CategoryDTO;
import com.kb.system.module.kb.category.entity.KbCategory;
import com.kb.system.module.kb.category.repository.KbCategoryRepository;
import com.kb.system.module.kb.category.vo.CategoryVO;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
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

/**
 * 分类管理服务：CRUD
 */
@Service
public class KbCategoryService {

    private final KbCategoryRepository categoryRepository;
    private final KbDocumentRepository documentRepository;

    public KbCategoryService(KbCategoryRepository categoryRepository,
                             KbDocumentRepository documentRepository) {
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
    }

    public PageResult<CategoryVO> page(CategoryDTO.Query query) {
        Specification<KbCategory> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + query.getName() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KbCategory> page = categoryRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "sort")));
        return PageResult.from(page, CategoryVO::from);
    }

    public List<CategoryVO> all() {
        return categoryRepository.findAllByOrderBySortAsc().stream()
                .map(CategoryVO::from).toList();
    }

    @Transactional
    public Long create(CategoryDTO.Create dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new BusinessException("分类名称已存在");
        }
        KbCategory category = new KbCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        return categoryRepository.save(category).getId();
    }

    @Transactional
    public void update(Long id, CategoryDTO.Update dto) {
        KbCategory category = getCategory(id);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        KbCategory category = getCategory(id);
        long referenced = documentRepository.findAll().stream()
                .filter(doc -> doc.getCategoryId().equals(id)).count();
        if (referenced > 0) {
            throw new BusinessException("该分类下还有 " + referenced + " 个文档，无法删除");
        }
        categoryRepository.delete(category);
    }

    private KbCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));
    }
}
