package com.kb.system.module.system.permission.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限模块请求对象集合
 */
public class PermissionDTO {

    /** 新增权限 */
    @Data
    public static class Create {
        @NotBlank(message = "权限编码不能为空")
        @Pattern(regexp = "^[a-zA-Z]+(:[a-zA-Z]+)+$", message = "权限编码格式如 kb:doc:add")
        private String permCode;

        @NotBlank(message = "权限名称不能为空")
        @Size(max = 50, message = "权限名称长度不能超过 50")
        private String permName;

        @NotBlank(message = "所属模块不能为空")
        @Size(max = 50, message = "模块名称长度不能超过 50")
        private String module;

        @NotNull(message = "权限类型不能为空")
        private Integer type;

        @Size(max = 100, message = "路由路径长度不能超过 100")
        private String routePath;
    }

    /** 修改权限 */
    @Data
    public static class Update {
        @NotBlank(message = "权限名称不能为空")
        @Size(max = 50, message = "权限名称长度不能超过 50")
        private String permName;

        @NotBlank(message = "所属模块不能为空")
        @Size(max = 50, message = "模块名称长度不能超过 50")
        private String module;

        @NotNull(message = "权限类型不能为空")
        private Integer type;

        @Size(max = 100, message = "路由路径长度不能超过 100")
        private String routePath;
    }

    /** 权限分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String module;
        private String keyword;
        private Integer type;
    }
}
