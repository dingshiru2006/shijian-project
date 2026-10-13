package com.kb.system.module.search.service;

import com.kb.system.module.kb.document.entity.KbDocument;
import com.kb.system.module.kb.document.entity.KbDocumentTag;
import com.kb.system.module.kb.document.repository.KbDocumentTagRepository;
import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import com.kb.system.module.kb.knowledgebase.repository.KbKnowledgeBaseRepository;
import com.kb.system.module.kb.category.entity.KbCategory;
import com.kb.system.module.kb.category.repository.KbCategoryRepository;
import com.kb.system.module.kb.tag.entity.KbTag;
import com.kb.system.module.kb.tag.repository.KbTagRepository;
import com.kb.system.module.search.vo.DocumentIndexVO;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.repository.SysUserRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ES 索引文档构建器：把数据库文档组装为可检索的索引文档（批量解析关联名称，避免 N+1）
 */
@Component
public class IndexDocumentBuilder {

    private final KbKnowledgeBaseRepository kbRepository;
    private final KbCategoryRepository categoryRepository;
    private final KbTagRepository tagRepository;
    private final KbDocumentTagRepository documentTagRepository;
    private final SysUserRepository userRepository;

    public IndexDocumentBuilder(KbKnowledgeBaseRepository kbRepository,
                                KbCategoryRepository categoryRepository,
                                KbTagRepository tagRepository,
                                KbDocumentTagRepository documentTagRepository,
                                SysUserRepository userRepository) {
        this.kbRepository = kbRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.documentTagRepository = documentTagRepository;
        this.userRepository = userRepository;
    }

    /** 批量构建（索引重建使用） */
    public List<DocumentIndexVO> buildBatch(List<KbDocument> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        Set<Long> kbIds = documents.stream().map(KbDocument::getKbId).collect(Collectors.toSet());
        Set<Long> categoryIds = documents.stream().map(KbDocument::getCategoryId).collect(Collectors.toSet());
        Set<Long> authorIds = documents.stream().map(KbDocument::getAuthorId).collect(Collectors.toSet());
        Set<Long> docIds = documents.stream().map(KbDocument::getId).collect(Collectors.toSet());

        Map<Long, String> kbNames = kbRepository.findAllById(kbIds).stream()
                .collect(Collectors.toMap(KbKnowledgeBase::getId, KbKnowledgeBase::getName, (a, b) -> a));
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(KbCategory::getId, KbCategory::getName, (a, b) -> a));
        Map<Long, String> authorNames = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));

        Map<Long, List<Long>> docTagIds = documentTagRepository.findByDocumentIdIn(docIds).stream()
                .collect(Collectors.groupingBy(KbDocumentTag::getDocumentId,
                        Collectors.mapping(KbDocumentTag::getTagId, Collectors.toList())));
        Set<Long> allTagIds = docTagIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> tagNames = allTagIds.isEmpty() ? Map.of()
                : tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(KbTag::getId, KbTag::getName, (a, b) -> a));

        return documents.stream().map(doc -> {
            List<String> tags = docTagIds.getOrDefault(doc.getId(), List.of()).stream()
                    .map(tagNames::get).filter(name -> name != null).toList();
            return build(doc, kbNames.get(doc.getKbId()), categoryNames.get(doc.getCategoryId()),
                    authorNames.get(doc.getAuthorId()), tags);
        }).toList();
    }

    /** 单个构建（文档增删改实时同步使用） */
    public DocumentIndexVO build(KbDocument doc) {
        String kbName = kbRepository.findById(doc.getKbId()).map(KbKnowledgeBase::getName).orElse("");
        String categoryName = categoryRepository.findById(doc.getCategoryId())
                .map(KbCategory::getName).orElse("");
        String authorName = userRepository.findById(doc.getAuthorId())
                .map(SysUser::getNickname).orElse("");
        List<Long> tagIds = documentTagRepository.findByDocumentId(doc.getId()).stream()
                .map(KbDocumentTag::getTagId).toList();
        List<String> tagNames = tagIds.isEmpty() ? List.of()
                : tagRepository.findAllById(tagIds).stream().map(KbTag::getName).toList();
        return build(doc, kbName, categoryName, authorName, tagNames);
    }

    private DocumentIndexVO build(KbDocument doc, String kbName, String categoryName,
                                  String authorName, List<String> tagNames) {
        Long createdAtMillis = doc.getCreatedAt() == null ? null
                : doc.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new DocumentIndexVO(doc.getId(), doc.getTitle(), doc.getSummary(), doc.getContent(),
                doc.getKbId(), kbName == null ? "" : kbName, doc.getCategoryId(),
                categoryName == null ? "" : categoryName,
                tagNames == null ? List.of() : tagNames, doc.getAuthorId(),
                authorName == null ? "" : authorName, doc.getStatus(), createdAtMillis);
    }
}
