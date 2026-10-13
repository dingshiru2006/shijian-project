package com.kb.system.module.system.role.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色模块请求对象集合
 */
public class RoleDTO {

    /** 新增角色 */
    @Data
    public static class Create {
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "角色编码需大写字母开头，可含数字、下划线，长度 2-50")
        private String roleCode;

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称长度不能超过 50")
        private String roleName;

        @Size(max = 255, message = "角色描述长度不能超过 255")
        private String description;

        @NotNull(message = "状态不能为空")
        private Integer status;

        @NotNull(message = "排序不能为空")
        private Integer sort;
    }

    /** 修改角色 */
    @Data
    public static class Update {
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称长度不能超过 50")
        private String roleName;

        @Size(max = 255, message = "角色描述长度不能超过 255")
        private String description;

        @NotNull(message = "状态不能为空")
        private Integer status;

        @NotNull(message = "排序不能为空")
        private Integer sort;
    }

    /** 分配权限 */
    @Data
    public static class AssignPermissions {
        private List<Long> permissionIds;
    }

    /** 角色分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String roleCode;
        private String roleName;
        private Integer status;
    }
}
