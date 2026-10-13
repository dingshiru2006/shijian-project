package com.kb.system.module.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Elasticsearch 文档索引结构（索引名 kb_document）
 * <p>
 * 注意：ES 检索时 Jackson 需要反序列化 _source 回本类，
 * 因此保留无参构造器（@NoArgsConstructor）与 setter（@Data 自带）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIndexVO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long kbId;
    private String kbName;
    private Long categoryId;
    private String categoryName;
    private List<String> tagNames;
    private Long authorId;
    private String authorName;
    private Integer status;
    /** 创建时间（epoch millis，映射为 date/epoch_millis） */
    private Long createdAt;
}
