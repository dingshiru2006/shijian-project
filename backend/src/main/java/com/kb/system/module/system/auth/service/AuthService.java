package com.kb.system.module.system.auth.service;

import com.kb.system.common.BusinessException;
import com.kb.system.module.system.auth.dto.LoginDTO;
import com.kb.system.module.system.auth.dto.PasswordUpdateDTO;
import com.kb.system.module.system.auth.vo.LoginVO;
import com.kb.system.module.system.user.entity.SysUser;
import com.kb.system.module.system.user.repository.SysUserRepository;
import com.kb.system.security.JwtUtils;
import com.kb.system.security.LoginUser;
import com.kb.system.security.UserDetailsServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 认证服务：登录（JWT + Redis 登录态）、当前用户信息、退出、修改密码
 */
@Service
public class AuthService {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String USER_KEY_PREFIX = "auth:user:";

    private final AuthenticationManager authenticationManager;
    private final SysUserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       SysUserRepository userRepository,
                       UserDetailsServiceImpl userDetailsService,
                       JwtUtils jwtUtils,
                       RedisTemplate<String, Object> redisTemplate,
                       BCryptPasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录：BCrypt 校验密码 -> 生成 JWT -> 登录态写入 Redis
     */
    @Transactional
    public LoginVO login(LoginDTO dto, String clientIp) {
        SysUser user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("用户名或密码错误");
        }

        // 生成 Token 与唯一登录标识 jti
        JwtUtils.GeneratedToken generated = jwtUtils.generate(user.getId(), user.getUsername());

        // 登录状态保存到 Redis（单点登录：同一账号新登录会使旧 Token 立即失效）
        Duration ttl = Duration.ofSeconds(generated.expiresIn());
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + user.getId(), generated.jti(), ttl);
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(user.getUsername());
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getId(), loginUser, ttl);

        // 记录最近登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userRepository.save(user);

        LoginVO.UserInfoVO userInfo = new LoginVO.UserInfoVO(
                user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                new ArrayList<>(loginUser.getRoles()), new ArrayList<>(loginUser.getPermissions()));
        return new LoginVO(generated.token(), "Bearer", generated.expiresIn(), userInfo);
    }

    /**
     * 退出登录：删除 Redis 登录态，Token 立即失效
     */
    public void logout(Long userId) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    /**
     * 修改密码：校验原密码 -> BCrypt 加密新密码 -> 清除登录态强制重新登录
     */
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        logout(userId);
    }
}
