package com.kb.system.module.kb.knowledgebase.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库模块请求对象集合
 */
public class KbDTO {

    /** 新增知识库 */
    @Data
    public static class Create {
        @NotBlank(message = "知识库名称不能为空")
        @Size(max = 100, message = "知识库名称长度不能超过 100")
        private String name;

        @NotBlank(message = "知识库编码不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{2,50}$", message = "编码只能包含字母、数字、中划线、下划线，长度 2-50")
        private String code;

        @Size(max = 500, message = "简介长度不能超过 500")
        private String description;

        @NotNull(message = "可见性不能为空")
        private Integer visibility;

        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    /** 修改知识库 */
    @Data
    public static class Update {
        @NotBlank(message = "知识库名称不能为空")
        @Size(max = 100, message = "知识库名称长度不能超过 100")
        private String name;

        @Size(max = 500, message = "简介长度不能超过 500")
        private String description;

        @NotNull(message = "可见性不能为空")
        private Integer visibility;

        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    /** 知识库分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String name;
        private String code;
        private Integer status;
    }
}
