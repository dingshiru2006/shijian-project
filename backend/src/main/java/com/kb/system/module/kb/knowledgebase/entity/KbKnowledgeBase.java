package com.kb.system.module.kb.knowledgebase.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库表
 */
@Entity
@Table(name = "kb_knowledge_base")
@Getter
@Setter
public class KbKnowledgeBase extends BaseEntity {

    /** 知识库名称 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 知识库编码（唯一） */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** 知识库简介 */
    @Column(name = "description", length = 500)
    private String description;

    /** 负责人 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 可见性：1-公开，2-私有 */
    @Column(name = "visibility", nullable = false)
    private Integer visibility;

    /** 文档数量（冗余统计） */
    @Column(name = "doc_count", nullable = false)
    private Integer docCount;

    /** 状态：1-启用，0-禁用 */
    @Column(name = "status", nullable = false)
    private Integer status;
}
