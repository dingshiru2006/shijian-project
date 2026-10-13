package com.kb.system.common;

/**
 * 操作类型（用于 AOP 操作日志）
 */
public enum OperationType {

    /** 登录 */
    LOGIN,
    /** 退出登录 */
    LOGOUT,
    /** 新增 */
    ADD,
    /** 修改 */
    UPDATE,
    /** 删除 */
    DELETE,
    /** 查询 */
    QUERY,
    /** 授权/分配 */
    ASSIGN,
    /** 上传 */
    UPLOAD,
    /** 索引重建 */
    REBUILD,
    /** 修改密码 */
    PASSWORD,
    /** 其他 */
    OTHER
}
