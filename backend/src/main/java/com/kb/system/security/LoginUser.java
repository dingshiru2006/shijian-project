package com.kb.system.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kb.system.module.system.user.entity.SysUser;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户主体：用户信息 + 角色 + 权限（供 Redis 缓存与安全上下文使用）
 * <p>
 * 注意：此类会被 Jackson 序列化/反序列化存入 Redis，
 * 因此需要保留无参构造器与 setter（缓存读取时反序列化使用）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private boolean enabled;
    /** 角色编码（不含 ROLE_ 前缀） */
    private Set<String> roles;
    /** 权限编码 */
    private Set<String> permissions;

    /** Jackson 反序列化需要 */
    public LoginUser() {
    }

    public LoginUser(SysUser user, Set<String> roles, Set<String> permissions) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.avatar = user.getAvatar();
        this.enabled = user.getStatus() != null && user.getStatus() == 1;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));
        return authorities;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
