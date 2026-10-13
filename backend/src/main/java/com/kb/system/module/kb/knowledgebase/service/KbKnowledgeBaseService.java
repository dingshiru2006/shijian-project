package com.kb.system.module.kb.knowledgebase.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.kb.document.entity.KbDocument;
import com.kb.system.module.kb.document.entity.KbDocumentTag;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
import com.kb.system.module.kb.document.repository.KbDocumentTagRepository;
import com.kb.system.module.kb.knowledgebase.dto.KbDTO;
import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import com.kb.system.module.kb.knowledgebase.repository.KbKnowledgeBaseRepository;
import com.kb.system.module.kb.knowledgebase.vo.KbVO;
import com.kb.system.module.search.service.DocumentSearchService;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.repository.SysUserRepository;
import com.kb.system.security.CurrentUser;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库管理服务：CRUD（删除时级联清理文档与 ES 索引）
 */
@Service
public class KbKnowledgeBaseService {

    private final KbKnowledgeBaseRepository kbRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentTagRepository documentTagRepository;
    private final DocumentSearchService searchService;
    private final SysUserRepository userRepository;
    private final CurrentUser currentUser;

    public KbKnowledgeBaseService(KbKnowledgeBaseRepository kbRepository,
                                  KbDocumentRepository documentRepository,
                                  KbDocumentTagRepository documentTagRepository,
                                  DocumentSearchService searchService,
                                  SysUserRepository userRepository,
                                  CurrentUser currentUser) {
        this.kbRepository = kbRepository;
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.searchService = searchService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public PageResult<KbVO> page(KbDTO.Query query) {
        Specification<KbKnowledgeBase> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + query.getName() + "%"));
            }
            if (StringUtils.hasText(query.getCode())) {
                predicates.add(cb.like(root.get("code"), "%" + query.getCode() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KbKnowledgeBase> page = kbRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, String> ownerNames = resolveOwnerNames(page.getContent());
        Map<Long, Long> docCounts = resolveDocCounts();
        return PageResult.from(page, kb -> {
            KbVO vo = KbVO.from(kb, ownerNames.get(kb.getOwnerId()));
            vo.setDocCount(docCounts.getOrDefault(kb.getId(), 0L).intValue());
            return vo;
        });
    }

    public List<KbVO> all() {
        List<KbKnowledgeBase> list = kbRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        Map<Long, String> ownerNames = resolveOwnerNames(list);
        Map<Long, Long> docCounts = resolveDocCounts();
        return list.stream().map(kb -> {
            KbVO vo = KbVO.from(kb, ownerNames.get(kb.getOwnerId()));
            vo.setDocCount(docCounts.getOrDefault(kb.getId(), 0L).intValue());
            return vo;
        }).toList();
    }

    public KbVO detail(Long id) {
        KbKnowledgeBase kb = getKb(id);
        String ownerName = userRepository.findById(kb.getOwnerId())
                .map(SysUser::getNickname).orElse(null);
        KbVO vo = KbVO.from(kb, ownerName);
        vo.setDocCount((int) documentRepository.countByKbId(id));
        return vo;
    }

    @Transactional
    public Long create(KbDTO.Create dto) {
        if (kbRepository.existsByCode(dto.getCode())) {
            throw new BusinessException("知识库编码已存在");
        }
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName(dto.getName());
        kb.setCode(dto.getCode());
        kb.setDescription(dto.getDescription());
        kb.setOwnerId(currentUser.id());
        kb.setVisibility(dto.getVisibility());
        kb.setDocCount(0);
        kb.setStatus(dto.getStatus());
        return kbRepository.save(kb).getId();
    }

    @Transactional
    public void update(Long id, KbDTO.Update dto) {
        KbKnowledgeBase kb = getKb(id);
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        kb.setVisibility(dto.getVisibility());
        kb.setStatus(dto.getStatus());
        kbRepository.save(kb);
    }

    /**
     * 删除知识库：级联删除其下文档、文档标签关联及 ES 索引数据
     */
    @Transactional
    public void delete(Long id) {
        KbKnowledgeBase kb = getKb(id);
        List<KbDocument> documents = documentRepository.findByKbId(id);
        for (KbDocument doc : documents) {
            documentTagRepository.deleteByDocumentId(doc.getId());
            searchService.deleteDocument(doc.getId());
        }
        documentRepository.deleteAll(documents);
        kbRepository.delete(kb);
    }

    /** 文档新增时同步知识库文档数 */
    @Transactional
    public void increaseDocCount(Long kbId) {
        kbRepository.findById(kbId).ifPresent(kb -> {
            kb.setDocCount((kb.getDocCount() == null ? 0 : kb.getDocCount()) + 1);
            kbRepository.save(kb);
        });
    }

    /** 文档删除时同步知识库文档数 */
    @Transactional
    public void decreaseDocCount(Long kbId) {
        kbRepository.findById(kbId).ifPresent(kb -> {
            kb.setDocCount(Math.max(0, (kb.getDocCount() == null ? 0 : kb.getDocCount()) - 1));
            kbRepository.save(kb);
        });
    }

    private KbKnowledgeBase getKb(Long id) {
        return kbRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库不存在"));
    }

    private Map<Long, String> resolveOwnerNames(List<KbKnowledgeBase> kbs) {
        Set<Long> ownerIds = kbs.stream().map(KbKnowledgeBase::getOwnerId).collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
    }

    /** 动态统计各知识库文档数（单次 GROUP BY 查询） */
    private Map<Long, Long> resolveDocCounts() {
        return documentRepository.countAllByKb().stream()
                .collect(Collectors.toMap(
                        KbDocumentRepository.KbCountProjection::getKbId,
                        KbDocumentRepository.KbCountProjection::getCnt,
                        Long::sum));
    }
}
