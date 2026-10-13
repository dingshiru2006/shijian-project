package com.kb.system.aop.annotation;

import com.kb.system.common.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解：标注重要写操作，由 AOP 切面异步记录
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 功能模块，如"文档管理" */
    String module();

    /** 操作类型 */
    OperationType type();

    /** 操作描述 */
    String description() default "";
}
