package com.kb.system.module.system.user.dto;

import com.kb.system.common.PageQuery;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户模块请求对象集合
 */
public class UserDTO {

    /** 新增用户 */
    @Data
    public static class Create {
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{2,50}$", message = "用户名只能包含字母、数字、下划线，长度 2-50")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 个字符之间")
        private String password;

        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称长度不能超过 50")
        private String nickname;

        @Email(message = "邮箱格式不正确")
        private String email;

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态取值 0 或 1")
        @Max(value = 1, message = "状态取值 0 或 1")
        private Integer status;

        /** 初始角色（可空） */
        private List<Long> roleIds;
    }

    /** 修改用户 */
    @Data
    public static class Update {
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称长度不能超过 50")
        private String nickname;

        @Email(message = "邮箱格式不正确")
        private String email;

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态取值 0 或 1")
        @Max(value = 1, message = "状态取值 0 或 1")
        private Integer status;
    }

    /** 分配角色 */
    @Data
    public static class AssignRoles {
        @NotEmpty(message = "角色不能为空")
        private List<Long> roleIds;
    }

    /** 管理员重置密码 */
    @Data
    public static class ResetPassword {
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度需在 6-32 个字符之间")
        private String newPassword;
    }

    /** 用户分页查询 */
    @Data
    public static class Query extends PageQuery {
        private String username;
        private String nickname;
        private Integer status;
        private Long roleId;
    }
}
