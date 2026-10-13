package com.kb.system.module.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 检索结果条目（含高亮字段）
 */
@Data
@AllArgsConstructor
public class SearchHitVO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long kbId;
    private String kbName;
    private Long categoryId;
    private String categoryName;
    private List<String> tagNames;
    private String authorName;
    private Integer status;
    /** 创建时间（格式化字符串） */
    private String createdAtText;
    /** 相关度得分 */
    private Double score;
    /** 高亮标题（缺失时回退为原标题） */
    private String highlightTitle;
    /** 高亮摘要片段 */
    private String highlightSummary;
    /** 高亮正文片段 */
    private String highlightContent;
}
