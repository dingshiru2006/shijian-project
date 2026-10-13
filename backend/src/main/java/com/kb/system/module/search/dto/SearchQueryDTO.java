package com.kb.system.module.search.dto;

import com.kb.system.common.PageQuery;
import lombok.Data;

/**
 * 全文检索查询条件：关键词 + 组合筛选
 */
@Data
public class SearchQueryDTO extends PageQuery {

    /** 搜索关键词（标题/摘要/正文） */
    private String keyword;

    /** 知识库 ID */
    private Long kbId;

    /** 分类 ID */
    private Long categoryId;

    /** 标签名称 */
    private String tagName;

    /** 文档状态 */
    private Integer status;

    /** 创建时间起 yyyy-MM-dd */
    private String startDate;

    /** 创建时间止 yyyy-MM-dd */
    private String endDate;
}
