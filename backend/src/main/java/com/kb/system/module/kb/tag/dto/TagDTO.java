package com.kb.system.module.kb.tag.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签模块请求对象集合
 */
public class TagDTO {

    /** 新增标签 */
    @Data
    public static class Create {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称长度不能超过 50")
        private String name;

        @Size(max = 20, message = "颜色值长度不能超过 20")
        private String color;
    }

    /** 修改标签 */
    @Data
    public static class Update {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称长度不能超过 50")
        private String name;

        @Size(max = 20, message = "颜色值长度不能超过 20")
        private String color;
    }

    /** 标签分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String name;
    }
}
