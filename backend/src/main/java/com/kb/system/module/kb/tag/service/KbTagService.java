package com.kb.system.module.kb.tag.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.kb.document.repository.KbDocumentTagRepository;
import com.kb.system.module.kb.tag.dto.TagDTO;
import com.kb.system.module.kb.tag.entity.KbTag;
import com.kb.system.module.kb.tag.repository.KbTagRepository;
import com.kb.system.module.kb.tag.vo.TagVO;
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
 * 标签管理服务：CRUD
 */
@Service
public class KbTagService {

    private final KbTagRepository tagRepository;
    private final KbDocumentTagRepository documentTagRepository;

    public KbTagService(KbTagRepository tagRepository,
                        KbDocumentTagRepository documentTagRepository) {
        this.tagRepository = tagRepository;
        this.documentTagRepository = documentTagRepository;
    }

    public PageResult<TagVO> page(TagDTO.Query query) {
        Specification<KbTag> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + query.getName() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KbTag> page = tagRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "id")));
        return PageResult.from(page, TagVO::from);
    }

    public List<TagVO> all() {
        return tagRepository.findAllByOrderByIdAsc().stream().map(TagVO::from).toList();
    }

    @Transactional
    public Long create(TagDTO.Create dto) {
        if (tagRepository.existsByName(dto.getName())) {
            throw new BusinessException("标签名称已存在");
        }
        KbTag tag = new KbTag();
        tag.setName(dto.getName());
        tag.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : "#409EFF");
        return tagRepository.save(tag).getId();
    }

    @Transactional
    public void update(Long id, TagDTO.Update dto) {
        KbTag tag = getTag(id);
        tag.setName(dto.getName());
        tag.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : tag.getColor());
        tagRepository.save(tag);
    }

    @Transactional
    public void delete(Long id) {
        KbTag tag = getTag(id);
        long referenced = documentTagRepository.findAll().stream()
                .filter(dt -> dt.getTagId().equals(id)).count();
        if (referenced > 0) {
            throw new BusinessException("该标签已被 " + referenced + " 个文档引用，无法删除");
        }
        tagRepository.delete(tag);
    }

    private KbTag getTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在"));
    }
}
