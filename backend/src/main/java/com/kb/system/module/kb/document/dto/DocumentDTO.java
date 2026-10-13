package com.kb.system.module.kb.document.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文档模块请求对象集合
 */
public class DocumentDTO {

    /** 新增文档 */
    @Data
    public static class Create {
        @NotNull(message = "所属知识库不能为空")
        private Long kbId;

        @NotNull(message = "所属分类不能为空")
        private Long categoryId;

        @NotBlank(message = "文档标题不能为空")
        @Size(max = 200, message = "文档标题长度不能超过 200")
        private String title;

        @Size(max = 500, message = "摘要长度不能超过 500")
        private String summary;

        @NotBlank(message = "文档内容不能为空")
        private String content;

        /** 附件文件 ID（可空） */
        private Long fileId;

        @NotNull(message = "状态不能为空")
        private Integer status;

        /** 标签 ID 列表 */
        private List<Long> tagIds;
    }

    /** 修改文档 */
    @Data
    public static class Update {
        @NotNull(message = "所属知识库不能为空")
        private Long kbId;

        @NotNull(message = "所属分类不能为空")
        private Long categoryId;

        @NotBlank(message = "文档标题不能为空")
        @Size(max = 200, message = "文档标题长度不能超过 200")
        private String title;

        @Size(max = 500, message = "摘要长度不能超过 500")
        private String summary;

        @NotBlank(message = "文档内容不能为空")
        private String content;

        private Long fileId;

        @NotNull(message = "状态不能为空")
        private Integer status;

        private List<Long> tagIds;
    }

    /** 文档分页查询（组合条件） */
    @Data
    public static class Query extends PageQuery {
        private String title;
        private Long kbId;
        private Long categoryId;
        private Long tagId;
        private Integer status;
    }
}
