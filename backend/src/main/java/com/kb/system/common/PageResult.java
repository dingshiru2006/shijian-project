package com.kb.system.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应格式
 */
@Getter
public class PageResult<T> {

    /** 当前页数据 */
    private final List<T> list;
    /** 总记录数 */
    private final long total;
    /** 当前页码 */
    private final int pageNum;
    /** 每页条数 */
    private final int pageSize;
    /** 总页数 */
    private final int pages;

    private PageResult(List<T> list, long total, int pageNum, int pageSize, int pages) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
    }

    /**
     * 将 JPA 分页结果转换为统一分页响应
     *
     * @param page      JPA 分页结果
     * @param converter 实体 -> VO 转换函数
     */
    public static <T, R> PageResult<T> from(Page<R> page, Function<R, T> converter) {
        List<T> list = page.getContent().stream().map(converter).toList();
        return new PageResult<>(list, page.getTotalElements(), page.getNumber() + 1,
                page.getSize(), page.getTotalPages());
    }

    /**
     * 直接构造分页响应（ES 检索等非 JPA 场景）
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        int pages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(list, total, pageNum, pageSize, pages);
    }
}
