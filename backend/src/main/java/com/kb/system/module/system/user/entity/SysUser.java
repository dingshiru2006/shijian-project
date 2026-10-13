package com.kb.system.module.system.user.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
public class SysUser extends BaseEntity {

    /** 登录账号 */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt 加密后的密码 */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /** 昵称 */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /** 邮箱 */
    @Column(name = "email", length = 100)
    private String email;

    /** 手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 头像地址 */
    @Column(name = "avatar", length = 255)
    private String avatar;

    /** 状态：1-启用，0-禁用 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 最近登录时间 */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 最近登录 IP */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
}
