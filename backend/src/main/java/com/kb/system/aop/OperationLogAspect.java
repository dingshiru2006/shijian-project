package com.kb.system.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.module.system.log.entity.SysOperationLog;
import com.kb.system.module.system.log.service.SysOperationLogService;
import com.kb.system.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志切面：拦截 @OperationLog 标注的方法，异步落库
 * <p>
 * 记录内容：模块、类型、描述、方法、请求地址/方式、请求参数、操作人、IP、耗时、结果、异常信息
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    private final ObjectMapper objectMapper;
    private final SysOperationLogService operationLogService;

    public OperationLogAspect(ObjectMapper objectMapper, SysOperationLogService operationLogService) {
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
        } catch (Throwable ex) {
            error = ex;
        }
        long cost = System.currentTimeMillis() - start;
        try {
            operationLogService.saveAsync(buildLog(point, operationLog, result, error, cost));
        } catch (Exception ex) {
            // 日志记录失败不影响业务
            log.warn("操作日志记录失败: {}", ex.getMessage());
        }
        if (error != null) {
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (error instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(error);
        }
        return result;
    }

    private SysOperationLog buildLog(ProceedingJoinPoint point, OperationLog operationLog,
                                     Object result, Throwable error, long cost) {
        SysOperationLog log = new SysOperationLog();
        log.setModule(operationLog.module());
        log.setOperationType(operationLog.type().name());
        log.setDescription(operationLog.description());
        log.setMethod(point.getSignature().getDeclaringTypeName() + "#" + point.getSignature().getName());
        log.setStatus(error == null ? 1 : 0);
        log.setCostTime(cost);
        if (error != null) {
            String message = error.getMessage();
            log.setErrorMsg(message == null ? error.getClass().getSimpleName()
                    : message.substring(0, Math.min(message.length(), 500)));
        }
        log.setRequestParams(serializeArgs(point.getArgs()));
        HttpServletRequest request = currentRequest();
        if (request != null) {
            log.setRequestUri(request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setOperatorIp(resolveIp(request));
        }
        fillOperator(log, point.getArgs());
        return log;
    }

    /** 操作人：优先取安全上下文（已登录），登录接口回退到请求参数中的 username */
    private void fillOperator(SysOperationLog log, Object[] args) {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof LoginUser loginUser) {
            log.setOperatorId(loginUser.getId());
            log.setOperator(loginUser.getNickname() == null ? loginUser.getUsername() : loginUser.getNickname());
            return;
        }
        Arrays.stream(args)
                .filter(arg -> arg != null && hasUsernameField(arg.getClass()))
                .findFirst()
                .ifPresent(arg -> {
                    try {
                        var field = arg.getClass().getDeclaredMethod("getUsername");
                        log.setOperator(String.valueOf(field.invoke(arg)));
                    } catch (Exception ignored) {
                        // 反射失败时忽略
                    }
                });
    }

    private boolean hasUsernameField(Class<?> clazz) {
        try {
            clazz.getDeclaredMethod("getUsername");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            String json = objectMapper.writeValueAsString(args);
            return json.length() > MAX_PARAM_LENGTH ? json.substring(0, MAX_PARAM_LENGTH) + "..." : json;
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
