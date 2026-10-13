package com.kb.system.module.kb.document.repository;

import com.kb.system.module.kb.document.entity.KbDocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 文档-标签关联 Repository
 */
public interface KbDocumentTagRepository extends JpaRepository<KbDocumentTag, Long> {

    List<KbDocumentTag> findByDocumentId(Long documentId);

    List<KbDocumentTag> findByDocumentIdIn(Collection<Long> documentIds);

    /**
     * 按文档删除标签关联。
     * 必须使用 @Modifying 批量删除：派生删除方法（SELECT + remove）会把 DELETE
     * 放入 Hibernate 动作队列，而同一事务中后续的 INSERT 先于 DELETE 执行，
     * 触发 uk_kb_document_tag 唯一键冲突。
     * flushAutomatically 确保事务内待刷新的更改（如文档更新）先落库。
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from KbDocumentTag t where t.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
