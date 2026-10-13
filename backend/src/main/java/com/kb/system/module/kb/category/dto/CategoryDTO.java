package com.kb.system.module.kb.category.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类模块请求对象集合
 */
public class CategoryDTO {

    /** 新增分类 */
    @Data
    public static class Create {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称长度不能超过 50")
        private String name;

        @Size(max = 255, message = "分类描述长度不能超过 255")
        private String description;

        @NotNull(message = "排序不能为空")
        private Integer sort;

        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    /** 修改分类 */
    @Data
    public static class Update {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称长度不能超过 50")
        private String name;

        @Size(max = 255, message = "分类描述长度不能超过 255")
        private String description;

        @NotNull(message = "排序不能为空")
        private Integer sort;

        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    /** 分类分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String name;
        private Integer status;
    }
}
