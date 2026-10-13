package com.kb.system.module.kb.document.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库文档表
 */
@Entity
@Table(name = "kb_document")
@Getter
@Setter
public class KbDocument extends BaseEntity {

    /** 所属知识库 ID */
    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    /** 所属分类 ID */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** 文档标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 文档摘要 */
    @Column(name = "summary", length = 500)
    private String summary;

    /** 文档正文 */
    @Lob
    @Column(name = "content", columnDefinition = "longtext")
    private String content;

    /** 关联附件文件 ID */
    @Column(name = "file_id")
    private Long fileId;

    /** 作者 ID */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 浏览次数 */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    /** 状态：1-已发布，0-草稿 */
    @Column(name = "status", nullable = false)
    private Integer status;
}
