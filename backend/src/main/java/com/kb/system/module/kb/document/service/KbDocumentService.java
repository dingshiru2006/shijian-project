package com.kb.system.module.kb.document.service;

import com.kb.system.common.BusinessException;
import com.kb.system.common.PageResult;
import com.kb.system.module.kb.category.entity.KbCategory;
import com.kb.system.module.kb.category.repository.KbCategoryRepository;
import com.kb.system.module.kb.document.dto.DocumentDTO;
import com.kb.system.module.kb.document.entity.KbDocument;
import com.kb.system.module.kb.document.entity.KbDocumentTag;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
import com.kb.system.module.kb.document.repository.KbDocumentTagRepository;
import com.kb.system.module.kb.document.vo.DocumentVO;
import com.kb.system.module.kb.file.entity.KbFile;
import com.kb.system.module.kb.file.repository.KbFileRepository;
import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import com.kb.system.module.kb.knowledgebase.repository.KbKnowledgeBaseRepository;
import com.kb.system.module.kb.knowledgebase.service.KbKnowledgeBaseService;
import com.kb.system.module.kb.tag.entity.KbTag;
import com.kb.system.module.kb.tag.repository.KbTagRepository;
import com.kb.system.module.search.service.DocumentSearchService;
import com.kb.system.module.search.service.IndexDocumentBuilder;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.repository.SysUserRepository;
import com.kb.system.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档管理服务：CRUD + 标签维护 + ES 实时同步
 */
@Service
public class KbDocumentService {

    private final KbDocumentRepository documentRepository;
    private final KbDocumentTagRepository documentTagRepository;
    private final KbTagRepository tagRepository;
    private final KbKnowledgeBaseRepository kbRepository;
    private final KbCategoryRepository categoryRepository;
    private final SysUserRepository userRepository;
    private final KbFileRepository fileRepository;
    private final KbKnowledgeBaseService knowledgeBaseService;
    private final DocumentSearchService searchService;
    private final IndexDocumentBuilder indexDocumentBuilder;
    private final CurrentUser currentUser;

    public KbDocumentService(KbDocumentRepository documentRepository,
                             KbDocumentTagRepository documentTagRepository,
                             KbTagRepository tagRepository,
                             KbKnowledgeBaseRepository kbRepository,
                             KbCategoryRepository categoryRepository,
                             SysUserRepository userRepository,
                             KbFileRepository fileRepository,
                             KbKnowledgeBaseService knowledgeBaseService,
                             DocumentSearchService searchService,
                             IndexDocumentBuilder indexDocumentBuilder,
                             CurrentUser currentUser) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.tagRepository = tagRepository;
        this.kbRepository = kbRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.searchService = searchService;
        this.indexDocumentBuilder = indexDocumentBuilder;
        this.currentUser = currentUser;
    }

    /**
     * 分页列表：标题模糊 + 知识库/分类/标签/状态组合筛选
     */
    public PageResult<DocumentVO> page(DocumentDTO.Query query) {
        Specification<KbDocument> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getTitle())) {
                predicates.add(cb.like(root.get("title"), "%" + query.getTitle() + "%"));
            }
            if (query.getKbId() != null) {
                predicates.add(cb.equal(root.get("kbId"), query.getKbId()));
            }
            if (query.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), query.getCategoryId()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getTagId() != null) {
                Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
                Root<KbDocumentTag> documentTag = subquery.from(KbDocumentTag.class);
                subquery.select(documentTag.get("documentId"))
                        .where(cb.equal(documentTag.get("tagId"), query.getTagId()));
                predicates.add(root.get("id").in(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KbDocument> page = documentRepository.findAll(specification,
                PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        DocumentNameResolver resolver = new DocumentNameResolver(page.getContent());
        return PageResult.from(page, doc -> DocumentVO.fromList(doc,
                resolver.kbName(doc.getKbId()), resolver.categoryName(doc.getCategoryId()),
                resolver.authorName(doc.getAuthorId()), resolver.fileName(doc.getFileId()),
                resolver.tagIds(doc.getId()), resolver.tagNames(doc.getId())));
    }

    /**
     * 文档详情（含正文）
     */
    public DocumentVO detail(Long id) {
        KbDocument doc = getDocument(id);
        DocumentNameResolver resolver = new DocumentNameResolver(List.of(doc));
        return DocumentVO.fromDetail(doc, resolver.kbName(doc.getKbId()),
                resolver.categoryName(doc.getCategoryId()), resolver.authorName(doc.getAuthorId()),
                resolver.fileName(doc.getFileId()), resolver.tagIds(doc.getId()),
                resolver.tagNames(doc.getId()));
    }

    @Transactional
    public Long create(DocumentDTO.Create dto) {
        validateReferences(dto.getKbId(), dto.getCategoryId(), dto.getTagIds());
        KbDocument doc = new KbDocument();
        doc.setKbId(dto.getKbId());
        doc.setCategoryId(dto.getCategoryId());
        doc.setTitle(dto.getTitle());
        doc.setSummary(dto.getSummary());
        doc.setContent(dto.getContent());
        doc.setFileId(dto.getFileId());
        doc.setAuthorId(currentUser.id());
        doc.setViewCount(0);
        doc.setStatus(dto.getStatus());
        KbDocument saved = documentRepository.save(doc);
        replaceTags(saved.getId(), dto.getTagIds());
        knowledgeBaseService.increaseDocCount(saved.getKbId());
        // 实时同步 ES 索引
        searchService.indexDocument(indexDocumentBuilder.build(saved));
        return saved.getId();
    }

    @Transactional
    public void update(Long id, DocumentDTO.Update dto) {
        KbDocument doc = getDocument(id);
        validateReferences(dto.getKbId(), dto.getCategoryId(), dto.getTagIds());
        Long oldKbId = doc.getKbId();
        doc.setKbId(dto.getKbId());
        doc.setCategoryId(dto.getCategoryId());
        doc.setTitle(dto.getTitle());
        doc.setSummary(dto.getSummary());
        doc.setContent(dto.getContent());
        doc.setFileId(dto.getFileId());
        doc.setStatus(dto.getStatus());
        documentRepository.save(doc);
        replaceTags(id, dto.getTagIds());
        if (!oldKbId.equals(dto.getKbId())) {
            knowledgeBaseService.decreaseDocCount(oldKbId);
            knowledgeBaseService.increaseDocCount(dto.getKbId());
        }
        // 实时同步 ES 索引
        searchService.indexDocument(indexDocumentBuilder.build(doc));
    }

    @Transactional
    public void delete(Long id) {
        KbDocument doc = getDocument(id);
        documentTagRepository.deleteByDocumentId(id);
        documentRepository.delete(doc);
        knowledgeBaseService.decreaseDocCount(doc.getKbId());
        // 同步删除 ES 索引
        searchService.deleteDocument(id);
    }

    private void validateReferences(Long kbId, Long categoryId, List<Long> tagIds) {
        if (kbRepository.findById(kbId).isEmpty()) {
            throw new BusinessException("所属知识库不存在");
        }
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new BusinessException("所属分类不存在");
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            Set<Long> distinctIds = new LinkedHashSet<>(tagIds);
            if (tagRepository.findAllById(distinctIds).size() != distinctIds.size()) {
                throw new BusinessException("包含不存在的标签");
            }
        }
    }

    private void replaceTags(Long documentId, List<Long> tagIds) {
        documentTagRepository.deleteByDocumentId(documentId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            KbDocumentTag documentTag = new KbDocumentTag();
            documentTag.setDocumentId(documentId);
            documentTag.setTagId(tagId);
            documentTagRepository.save(documentTag);
        }
    }

    private KbDocument getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    /**
     * 批量解析文档关联名称（知识库/分类/作者/附件/标签），避免逐条查询
     */
    private class DocumentNameResolver {
        private final Map<Long, String> kbNames;
        private final Map<Long, String> categoryNames;
        private final Map<Long, String> authorNames;
        private final Map<Long, String> fileNames;
        private final Map<Long, List<Long>> tagIdsByDoc;
        private final Map<Long, String> tagNames;

        DocumentNameResolver(List<KbDocument> docs) {
            kbNames = toNameMap(kbRepository.findAllById(ids(docs, KbDocument::getKbId)),
                    KbKnowledgeBase::getId, KbKnowledgeBase::getName);
            categoryNames = toNameMap(categoryRepository.findAllById(ids(docs, KbDocument::getCategoryId)),
                    KbCategory::getId, KbCategory::getName);
            authorNames = toNameMap(userRepository.findAllById(ids(docs, KbDocument::getAuthorId)),
                    SysUser::getId, SysUser::getNickname);
            fileNames = toNameMap(fileRepository.findAllById(docs.stream()
                            .map(KbDocument::getFileId).filter(java.util.Objects::nonNull)
                            .collect(Collectors.toSet())),
                    KbFile::getId, KbFile::getOriginalName);
            tagIdsByDoc = documentTagRepository.findByDocumentIdIn(
                            docs.stream().map(KbDocument::getId).collect(Collectors.toSet())).stream()
                    .collect(Collectors.groupingBy(KbDocumentTag::getDocumentId,
                            Collectors.mapping(KbDocumentTag::getTagId, Collectors.toList())));
            tagNames = tagIdsByDoc.values().stream().flatMap(List::stream).distinct()
                    .collect(Collectors.toMap(tagId -> tagId,
                            tagId -> tagRepository.findById(tagId).map(KbTag::getName).orElse(""),
                            (a, b) -> a));
        }

        String kbName(Long kbId) {
            return kbNames.getOrDefault(kbId, "");
        }

        String categoryName(Long categoryId) {
            return categoryNames.getOrDefault(categoryId, "");
        }

        String authorName(Long authorId) {
            return authorNames.getOrDefault(authorId, "");
        }

        String fileName(Long fileId) {
            return fileId == null ? null : fileNames.get(fileId);
        }

        List<Long> tagIds(Long documentId) {
            return tagIdsByDoc.getOrDefault(documentId, List.of());
        }

        List<String> tagNames(Long documentId) {
            return tagIdsByDoc.getOrDefault(documentId, List.of()).stream()
                    .map(tagNames::get).filter(name -> name != null).toList();
        }

        private <T> Set<Long> ids(List<KbDocument> docs, java.util.function.Function<KbDocument, T> getter) {
            return docs.stream().map(getter).filter(java.util.Objects::nonNull)
                    .map(id -> (Long) id).collect(Collectors.toSet());
        }

        private <E> Map<Long, String> toNameMap(List<E> entities,
                                                java.util.function.Function<E, Long> idGetter,
                                                java.util.function.Function<E, String> nameGetter) {
            return entities.stream().collect(Collectors.toMap(idGetter, nameGetter, (a, b) -> a));
        }
    }
}
