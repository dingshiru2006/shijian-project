package com.kb.system.module.kb.document.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档与标签关联表（多对多）
 */
@Entity
@Table(name = "kb_document_tag")
@Getter
@Setter
public class KbDocumentTag extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}
